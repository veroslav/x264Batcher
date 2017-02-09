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

import javafx.concurrent.Task;
import org.matic.x264batcher.gui.log.EncoderLogger;
import org.matic.x264batcher.gui.log.LogEntry.Severity;
import org.matic.x264batcher.model.AvsSegment;
import org.matic.x264batcher.model.EncoderJob;
import org.matic.x264batcher.model.EncoderParameters;
import org.matic.x264batcher.model.EncodingProgressView;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * A job encoding task. It controls the x264.exe encodings and merging of the
 * resulting x264 files. It also provides the progress status updates to the GUI.
 * 
 * @author Vedran Matic
 *
 */
final class EncodingTask extends Task<Void> {
	
	private final EncoderParameters encoderParameters;
	private final EncoderJob encoderJob;
	private final EncoderLogger logger;
	
	private final AvsEncoder encoder;  
	
	/**
	 * Create a new instance of the encoding task.
	 * 
	 * @param encoderParameters x264.exe executable parameters
	 * @param encoderJob A view to the corresponding encoder job
	 * @param logger Log encoding output to this logger
	 */
	EncodingTask(final EncoderParameters encoderParameters,
				 final EncoderJob encoderJob, final EncoderLogger logger) {
		this.encoderParameters = encoderParameters;
		this.encoderJob = encoderJob;
		this.logger = logger;
		
		final long totalFrames = this.encoderJob.getSegments().stream().mapToLong(AvsSegment::getFrameCount).sum();
		encoder = new AvsEncoder(this.encoderParameters.getEncoderJobsLimit(), totalFrames, logger);
	}

	EncodingProgressView getProgressView() {
		return encoder.getJobProgress();
	}

	@Override
	protected Void call() throws Exception {
		
		final String jobName = encoderJob.getJobParameters().getName();
		
		logger.log(Severity.INFO, "Start encoding: Job = " + jobName);
		
		//Encode job file segments
		final List<AvsSegment> avsSegments = encoderJob.getSegments();	
		encoder.encode(avsSegments.stream().map(AvsSegment::getCommand).collect(Collectors.toList()));
		
		logger.log(Severity.INFO, "All segments encoded: Job = " + jobName);
		
		//Merge encoded segment files
		final SegmentMerger mergerJob = new SegmentMerger(avsSegments,
				encoderJob.getJobParameters(), encoderParameters.getMkvMergeExecutablePath());
		
		logger.log(Severity.INFO, "Merging segments: Job = " + jobName + ", command = [ " +
				mergerJob.getCommand() + " ]");

		CompletableFuture.runAsync(mergerJob).join();

		//Check for any merger error, re-throw it if it exists
		final Exception mergeException = mergerJob.getError();
		if(mergeException != null) {
			
			logger.log(Severity.ERROR, "Merging segments failed: Job = " + jobName + ", due to = [ " +
					mergeException.getMessage() + " ]");
			
			throw mergeException;
		}
		
		logger.log(Severity.INFO, "Segments were merged: Job = " + jobName + ", command = [ " +
				mergerJob.getCommand() + " ]");
		
		return null;
	}
	
	@Override
	protected void cancelled() {		
		super.cancelled();
		encoder.cancel();			
	}
}