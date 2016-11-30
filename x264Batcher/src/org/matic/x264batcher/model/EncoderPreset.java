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
 * An encoder preset consists of a combination of x264.exe parameters that are
 * stored and referred to with a symbolic name. They can be reused between
 * the encodings. The user can create own presets or use the default one.  
 * 
 * @author Vedran Matic
 *
 */
public final class EncoderPreset {

	private static final String DEFAULT_PRESET_NAME = "Default";
	private static final EncoderPreset DEFAULT_ENCODER_PRESET = new EncoderPreset(
			DEFAULT_PRESET_NAME, EncoderPreset.buildDefaultEncoderPresetCommand());

	private final String presetName;
	private final String presetCommand;

	/**
	 * Create a new encoder preset instance.
	 * 
	 * @param presetName A symbolic name of this preset
	 * @param presetCommand The x264.exe command associated with this preset
	 */
	public EncoderPreset(final String presetName, final String presetCommand) {
		this.presetName = presetName;
		this.presetCommand = presetCommand;
	}

	public String getName() {
		return presetName;
	}

	public String getCommand() {
		return presetCommand;
	}
	
	/**
	 * Default encoder preset to use if the user hasn't yet created their own preset(s).
	 * 
	 * @return Default encoder preset
	 */
	public static EncoderPreset getDefault() {
		return DEFAULT_ENCODER_PRESET;
	}
	
	private static String buildDefaultEncoderPresetCommand() {
		final StringBuilder command = new StringBuilder();
		
		command.append("--level 4.1 --preset placebo --cabac --ref 5 --deblock -3:-3")
			.append(" --partitions all --me umh --subme 8 --psy-rd 1.00:0.00")
			.append(" --merange 24 --trellis 2 --8x8dct")
			.append(" --cqm flat --deadzone-inter 21 --deadzone-intra 11")
			.append(" --chroma-qp-offset 0")
			.append(" --threads 12 --lookahead-threads 2")
			.append(" --no-dct-decimate")
			.append(" --bframes 6 --b-pyramid normal --b-adapt 2 --b-bias 0 --direct auto")
			.append(" --weightp 2 --keyint 500 --min-keyint 50")
			.append(" --scenecut 40 --rc-lookahead 40")
			.append(" --crf 20.0 --qcomp 0.60 --qpmin 10 --qpmax 51 --qpstep 4")
			.append(" --ipratio 1.40 --aq-mode 1 --aq-strength 1.00");
		
		return command.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((presetName == null) ? 0 : presetName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EncoderPreset other = (EncoderPreset) obj;
		if (presetName == null) {
			if (other.presetName != null)
				return false;
		} else if (!presetName.equals(other.presetName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return presetName;
	}
}
