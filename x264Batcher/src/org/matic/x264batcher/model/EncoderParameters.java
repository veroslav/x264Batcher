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

/**
 * General encoder parameters, shared between all encoder jobs
 * 
 * @author Vedran Matic
 *
 */
public final class EncoderParameters {
	
	public static final String AUTO_JOB_LIMIT = "Auto";
	
	private final String mkvMergeExecutablePath;
	private final String x264ExecutablePath;
	private final int encoderJobsLimit;

	/**
	 * Create encoder parameters
	 * 
	 * @param x264ExecutablePath Path to the x264.exe file
	 * @param mkvMergeExecutablePath Path to the mkvmerge.exe file
	 * @param encoderJobsLimit Max parallel jobs (0 = Determine automatically)
	 */
	public EncoderParameters(final String x264ExecutablePath,
			final String mkvMergeExecutablePath,
			final int encoderJobsLimit) {
		this.mkvMergeExecutablePath = mkvMergeExecutablePath;
		this.x264ExecutablePath = x264ExecutablePath;
		this.encoderJobsLimit = encoderJobsLimit;
	}

	public final String getMkvMergeExecutablePath() {
		return mkvMergeExecutablePath;
	}

	public final String getX264ExecutablePath() {
		return x264ExecutablePath;
	}

	public final int getEncoderJobsLimit() {
		return encoderJobsLimit;
	}	
}