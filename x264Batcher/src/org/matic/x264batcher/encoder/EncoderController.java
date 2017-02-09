/*
* This file is part of x264Batcher, an x264 encoder multiplier written in JavaFX.
* Copyright (C) 2016 Vedran Matic
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
*
*/
package org.matic.x264batcher.encoder;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.matic.x264batcher.gui.log.EncoderLogger;
import org.matic.x264batcher.gui.log.LogEntry.Severity;
import org.matic.x264batcher.gui.ProgressPoller;
import org.matic.x264batcher.gui.model.QueuedJob;
import org.matic.x264batcher.model.AvsSegment;
import org.matic.x264batcher.model.EncoderJob;
import org.matic.x264batcher.model.EncoderParameters;
import org.matic.x264batcher.model.EncodingProgressView;
import org.matic.x264batcher.model.JobStatus;

import javafx.application.Platform;
import javafx.util.Duration;

/**
 * A controller for managing addition, removal and cancellation of encoding jobs.
 * Notifies listeners of encoding progress.  
 * Allows to start encoding of previously added jobs.
 * 
 * @author Vedran Matic
 *
 */
public final class EncoderController {
	
	private final List<EncodingProgressListener> listeners = new CopyOnWriteArrayList<>();
	private final Map<QueuedJob, EncoderJob> queuedJobs = new LinkedHashMap<>();
	
	private final EncoderLogger logger;
	
	private volatile EncodingTask currentEncoderTask = null;
	private ExecutorService encoderExecutor;
	
	private long totalFrameCount = 0;

	public EncoderController(final EncoderLogger logger) {
		this.logger = logger;
	}
	
	public void addListener(final EncodingProgressListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(final EncodingProgressListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Queue a job and make it eligible for encoding.
	 * 
	 * @param queuedJob Job to be added
	 */
	public void add(final QueuedJob queuedJob) {
		synchronized(queuedJobs) {
			queuedJobs.put(queuedJob, null);
			totalFrameCount += queuedJob.getInputAvsFiles().stream().mapToLong(f ->
					f.isUsingDeinterlacing()? 2 * f.getFrameCount() : f.getFrameCount()).sum();
		}
	}
	
	/**
	 * Remove queued jobs from the encoding queue. If any job is already running,
	 * it will be cancelled first. 
	 * 
	 * @param jobsToRemove A list of queued jobs to remove
	 * @return Whether any jobs were actually removed
	 */
	public boolean remove(final List<QueuedJob> jobsToRemove) {
		synchronized(queuedJobs) {
			jobsToRemove.forEach(queuedJobs::remove);
			
			totalFrameCount -= jobsToRemove.stream().flatMap(
					j -> j.getInputAvsFiles().stream()).mapToLong(avs -> 
					avs.isUsingDeinterlacing()? 2 * avs.getFrameCount() : avs.getFrameCount()).sum();
			
			final Optional<QueuedJob> jobInProgress = jobsToRemove.stream().filter(
					j -> j.getJobStatus() == JobStatus.RUNNING).findAny();
			jobInProgress.ifPresent(j -> cancel());
			
			return queuedJobs.isEmpty();
		}
	}
	
	/**
	 * Cancel currently running job, if any. If there are more jobs on the
	 * queue, the encoder will continue encoding these. Otherwise, it simply
	 * returns as there is nothing more to do. It will also notify any
	 * listeners if the job was successfully cancelled. 
	 */
	public void cancel() {
		synchronized(queuedJobs) {
			if(currentEncoderTask != null) {
				currentEncoderTask.cancel();
			}
		}
	}
	
	/**
	 * Cancel currently running job, if any, and set the status of any
	 * not yet started jobs to CANCELLED. After notifying any listeners
	 * of cancellations, it will return and notify the listeners again
	 * that all of the jobs in the queue have completed.
	 */
	public void cancelAll() {
		synchronized(queuedJobs) {
			if(encoderExecutor != null) {
				encoderExecutor.shutdownNow();
			}
		}
	}
	
	/**
	 * Start a new encoding of queued jobs, one by one, in the order they were added in. 
	 * 
	 * @param encoderParameters x264.exe executable command parameters
	 */
	public void encode(final EncoderParameters encoderParameters) {
		synchronized(queuedJobs) {
			if(encoderExecutor != null) {
				return;
			}
			encoderExecutor = Executors.newSingleThreadExecutor(r -> {
				final Thread thread = new Thread(r);
				thread.setDaemon(true);
				return thread;
			});
		}
		
		encoderExecutor.submit(() -> encodeJobs(encoderParameters));						
	}
	
	private List<QueuedJob> filterJobs(final Predicate<QueuedJob> jobStatus) {
		synchronized(queuedJobs) {
			return queuedJobs.keySet().stream().filter(jobStatus).collect(Collectors.toList());
		}
	}
	
	private void encodeJobs(final EncoderParameters encoderParameters) {				
		while(true) {
			synchronized(queuedJobs) {
				final List<QueuedJob> availableJobs = filterJobs(q -> q.getJobStatus() == JobStatus.QUEUED);
				if(availableJobs.isEmpty()) {
					//No more jobs left to encode, we are done
					encoderExecutor.shutdown();				
					break;
				}
				final QueuedJob nextJob = availableJobs.get(0);			
				nextJob.setStatus(JobStatus.RUNNING);
				nextJob.setMessage("");
				
				final SegmentBuilder segmentBuilder = new SegmentBuilder(encoderParameters, nextJob, logger);
				List<AvsSegment> avsSegments;
				try {
					avsSegments = segmentBuilder.buildSegments();
				} catch(final IOException ioe) {
					updateJobStatusOnCompletion(nextJob, JobStatus.FAILED,
							"Failed to build segments due to: " + ioe.getMessage());
					listeners.forEach(l -> l.onJobCompleted(nextJob));
					continue;
				}
				
				final EncoderJob encoderJob = new EncoderJob(nextJob, avsSegments);
				queuedJobs.put(nextJob, encoderJob);
				
				final EncodingTask encoderTask = new EncodingTask(encoderParameters, encoderJob, logger);
				currentEncoderTask = encoderTask;
				
				nextJob.setTimeStarted(System.currentTimeMillis());
				
				runJob(encoderJob, encoderTask);
				try {
					queuedJobs.wait();
				} catch(final InterruptedException ie) {
					Thread.interrupted();
					
					//Check whether the user cancelled the encoding
					if(encoderExecutor.isShutdown()) {
						
						System.out.println("Interrupted, all encoding tasks were cancelled");
						
						//Cancel the active encoding task, if any
						cancel();
						
						//Cancel all queued jobs
						filterJobs(j -> j.getJobStatus() == JobStatus.QUEUED).forEach(j -> {
							j.setStatus(JobStatus.CANCELLED);
							j.setMessage("");
						});
						break;
					}					
				}			
			}
		}
		encoderExecutor = null;
		Platform.runLater(() -> listeners.forEach(EncodingProgressListener::onAllJobsCompleted));
	}

	private void runJob(final EncoderJob encoderJob, final EncodingTask encoderTask) {
		final ExecutorService encoderTaskExecutor = Executors.newSingleThreadExecutor(r -> {
			final Thread thread = new Thread(r);
			thread.setDaemon(true);
			return thread;
		});
		
		final ProgressPoller progressPoller = new ProgressPoller(() ->
			listeners.forEach(l -> {
				final EncodingProgressView progressView = encoderTask.getProgressView();
				updateTotalProgress(progressView);
				l.onProgressUpdate(encoderJob.getQueuedJob(), progressView);
			}));
		
		progressPoller.setPeriod(Duration.seconds(1));
		
		encoderTask.setOnSucceeded(handler -> {
			logger.log(Severity.INFO, "Job completed: " + encoderJob.getJobParameters().getName());
			
			updateJobStatusOnCompletion(encoderJob.getQueuedJob(), JobStatus.FINISHED, "Completed");
			listeners.forEach(l -> l.onJobCompleted(encoderJob.getQueuedJob()));
			resetState(encoderTaskExecutor, progressPoller);
		});
		
		encoderTask.setOnCancelled(handler -> {
			logger.log(Severity.WARN, "Job was cancelled: " + encoderJob.getJobParameters().getName());

			updateJobStatusOnCompletion(encoderJob.getQueuedJob(), JobStatus.CANCELLED, "");
			listeners.forEach(l -> l.onJobCompleted(encoderJob.getQueuedJob()));
			resetState(encoderTaskExecutor, progressPoller);						
		});
		
		encoderTask.setOnFailed(handler -> {			
			final Throwable error = handler.getSource().getException();
			
			logger.log(Severity.ERROR, "Job failed: " + encoderJob.getJobParameters().getName() + ", cause = [ " +
					error.getMessage() + " ]");
			
			updateJobStatusOnCompletion(encoderJob.getQueuedJob(), JobStatus.FAILED, error.toString());
			listeners.forEach(l -> l.onJobCompleted(encoderJob.getQueuedJob()));
			resetState(encoderTaskExecutor, progressPoller);									
		});
		
		progressPoller.start();
		
		logger.log(Severity.INFO, "Start encoding: job = " + encoderJob.getJobParameters().getName());
		
		currentEncoderTask = encoderTask;
		encoderTaskExecutor.execute(encoderTask);
	}
	
	private void updateJobStatusOnCompletion(final QueuedJob queuedJob, final JobStatus completionStatus,
			final String message) {
		queuedJob.setTimeCompleted(System.currentTimeMillis());		
		queuedJob.setStatus(completionStatus);
		queuedJob.setMessage(message);
	}
	
	private void updateTotalProgress(final EncodingProgressView progressView) {
		final long totalFramesDone = filterJobs(j -> j.getJobStatus() != JobStatus.RUNNING).stream().mapToLong(
				q -> {
					final EncoderJob encoderJob = queuedJobs.get(q);
					return encoderJob != null? encoderJob.getFrameCount() : 0;
				}).sum() + progressView.getCurrentJobFramesDone();
				 
		progressView.setTotalFrames(totalFrameCount);
		progressView.setTotalFramesDone(totalFramesDone);
		
		final int totalJobsDone = filterJobs(j -> j.getJobStatus() != JobStatus.RUNNING &&
				j.getJobStatus() != JobStatus.QUEUED).size();
		
		progressView.setTotalJobs(queuedJobs.size());
		progressView.setTotalJobsDone(totalJobsDone);
	}
	
	private void resetState(final ExecutorService encoderTaskExecutor, final ProgressPoller progressPoller) {
		synchronized(queuedJobs) {
			currentEncoderTask = null;
			progressPoller.cancel();
			encoderTaskExecutor.shutdownNow();
			queuedJobs.notifyAll();
		}
	}
}