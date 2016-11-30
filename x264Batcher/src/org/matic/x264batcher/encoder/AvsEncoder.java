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

import org.matic.x264batcher.encoder.log.EncoderLogger;
import org.matic.x264batcher.encoder.log.LogEntry.Severity;
import org.matic.x264batcher.model.EncodingProgressView;
import org.matic.x264batcher.model.SegmentEncoderResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The x264 encoding process. It parallelizes input AVS files for more efficient encoding.
 * It offers means to listen for encoding progress updates.
 * 
 * @author Vedran Matic
 *
 */
final class AvsEncoder {
	
	private final List<SegmentEncoder> jobSegments = new ArrayList<>();
	
	private final ExecutorService jobExecutor;
	private final EncodingProgressView jobProgress;
	private final EncoderLogger logger;

	/**
	 * Create a new instance of the encoding process.
	 * 
	 * @param threadCount Limit of parallel encoding processes.
	 * @param frameCount Total frames to be encoded (used for progress updates)
	 * @param logger Logger to which the output progress info is written
	 */
	AvsEncoder(final int threadCount, final long frameCount, final EncoderLogger logger) {
		this.jobProgress = new EncodingProgressView(frameCount);
		this.jobExecutor = Executors.newFixedThreadPool(threadCount);
		this.logger = logger;
	}
	
	/**
	 * Cancel the encoding in progress.
	 */
	void cancel() {
		jobExecutor.shutdownNow();
	}
	
	/**
	 * Get a snapshot of encoding progress status.
	 * 
	 * @return Progress status
	 */
	EncodingProgressView getJobProgress() {
		double fps = 0;
		long totalFramesDone = 0;
		for(final SegmentEncoder job : jobSegments) {
			final String jobOutput = job.getOutput();
			
			if(jobOutput == null || jobOutput.trim().isEmpty()) {
				continue;
			}
			long framesDone = 0;
			if(jobOutput.startsWith("[")) {
				final String[] tokens = jobOutput.split(",");
				final String framesProgress = tokens[0].split(" ")[1];
				framesDone = Long.parseLong(
						framesProgress.substring(0, framesProgress.indexOf("/")));
				fps += Double.parseDouble(tokens[1].substring(0, tokens[1].indexOf(" fps")));
			}
			else if(jobOutput.startsWith("encoded ")) {
				framesDone = Long.parseLong(jobOutput.split(" ")[1]);
				fps += 0;
			}

			job.setFramesDone(job.getFramesDone() + (framesDone - job.getFramesDone()));
			totalFramesDone += job.getFramesDone();
		}
		
		jobProgress.setFps(fps);
		jobProgress.setCurrentJobFramesDone(totalFramesDone);
		
		return jobProgress;
	}
	
	/**
	 * Start an encoding process.
	 * 
	 * @param jobCommands x264.exe commands for used for segment encoding
	 * @throws InterruptedException If the encoding is interrupted/cancelled
	 */
	void encode(final List<String> jobCommands) throws InterruptedException {
		jobCommands.forEach(cmd -> jobSegments.add(new SegmentEncoder(cmd, logger)));

		//Encode segments to x264
		final List<Future<SegmentEncoderResult>> completedTasks = jobExecutor.invokeAll(jobSegments);
		jobExecutor.shutdownNow();
		
		logger.log(Severity.INFO, "Encoding completed [ " + jobCommands.size() + " segments encoded ]");
	}
}