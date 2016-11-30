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

import org.matic.x264batcher.utils.Helper;

import java.util.ArrayList;
import java.util.List;

/**
 * All of the job settings set by the user when creating an encoding job.
 * 
 * @author Vedran Matic
 *
 */
public final class EncoderJobParameters {
	
	private final List<String> jobInputPaths;
	private final boolean performCleanup;
	private final EncoderPreset encoderPreset;	
	private final String jobOutputPath;
	private final String outputSar;
	private final String name;

	/**
	 * Create a new instance of job parameters.
	 * 
	 * @param name Optional job name, also used as the encoded clip name
	 * @param jobOutputPath Directory to store the encoded clip in
	 * @param outputSar Clip's SAR value that is passed to x264
	 * @param jobInputPaths A list of paths to AVS files that are part of the job
	 * @param encoderPreset Encoder command line preset
	 * @param performCleanup Whether to remove temporary files after the encoding is done
	 */
	public EncoderJobParameters(final String name, final String jobOutputPath, final String outputSar,
			final List<String> jobInputPaths, final EncoderPreset encoderPreset, final boolean performCleanup) {
		this.performCleanup = performCleanup;
		this.encoderPreset = encoderPreset;		
		this.jobOutputPath = jobOutputPath;
		this.jobInputPaths = jobInputPaths;
		this.outputSar = outputSar;
		this.name = name;
	}
	
	/**
	 * A selection of default parameter values that are used when creating a new job.
	 * 
	 * @return Default parameter values
	 */
	public static EncoderJobParameters getDefault() {
		return new EncoderJobParameters("", Helper.loadPreference(Helper.LAST_OUTPUT_PATH_PROPERTY,
				System.getProperty("user.home")),
				Helper.loadPreference(Helper.SAR_NOMINATOR_PROPERTY, "16") + ":" +
						Helper.loadPreference(Helper.SAR_DENOMINATOR_PROPERTY, "15"),
				new ArrayList<>(),
				Helper.loadActiveEncoderPreset(),
				Boolean.parseBoolean(
						Helper.loadPreference(Helper.PERFORM_CLEANUP_PROPERTY, "true")));
	}

	public String getName() {
		return name;
	}

	public String getJobOutputPath() {
		return jobOutputPath;
	}
	
	public List<String> getJobInputPaths() {
		return jobInputPaths;
	}

	public boolean isDeleteTemporaryFiles() {
		return performCleanup;
	}

	public EncoderPreset getEncoderPreset() {
		return encoderPreset;
	}

	public String getOutputSar() {
		return outputSar;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (performCleanup ? 1231 : 1237);
		result = prime * result + ((encoderPreset == null) ? 0 : encoderPreset.hashCode());
		result = prime * result + ((jobOutputPath == null) ? 0 : jobOutputPath.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((outputSar == null) ? 0 : outputSar.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EncoderJobParameters other = (EncoderJobParameters) obj;
		if (performCleanup != other.performCleanup)
			return false;
		if (encoderPreset != other.encoderPreset)
			return false;
		if (jobOutputPath == null) {
			if (other.jobOutputPath != null)
				return false;
		} else if (!jobOutputPath.equals(other.jobOutputPath))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (outputSar == null) {
			if (other.outputSar != null)
				return false;
		} else if (!outputSar.equals(other.outputSar))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "EncoderJobParameters [cleanupIntermediateFiles=" + performCleanup + ", encoderPreset="
				+ encoderPreset + ", jobInputPath=" + jobOutputPath + ", outputSar=" + outputSar + ", name=" + name
				+ "]";
	}
}
