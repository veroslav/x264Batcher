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
package org.matic.x264batcher.model;

import org.matic.x264batcher.gui.model.QueuedJob;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * A view to an encoder job. Essentially a wrapper around {@link QueuedJob}
 * in order to encapsulate and hide it from the encoder layer.
 * 
 * @author Vedran Matic
 *
 */
public final class EncoderJob {
	
	private final QueuedJob queuedJob;	
	private final List<AvsSegment> segments;
	
	private final Path workPath;
	private final long frameCount; 

	/**
	 * Create an instance of the encoder job wrapper.
	 * 
	 * @param queuedJob The GUI layer view to the encoder job
	 * @param segments AVS script segments to be encoded
	 */
	public EncoderJob(final QueuedJob queuedJob, final List<AvsSegment> segments) {
		this.queuedJob = queuedJob;
		this.segments = segments;
		
		this.workPath = Paths.get(this.queuedJob.getJobParameters().getJobOutputPath());
		this.frameCount = segments.stream().mapToLong(AvsSegment::getFrameCount).sum();
	}
	
	public EncoderJobParameters getJobParameters() {
		return this.queuedJob.getJobParameters();
	}
	
	public QueuedJob getQueuedJob() {
		return queuedJob;
	}

	public List<AvsSegment> getSegments() {
		return segments;
	}
	
	public long getFrameCount() {
		return frameCount;
	}
}