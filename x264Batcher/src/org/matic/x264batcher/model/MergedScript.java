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

import java.util.List;

/**
 * A result of merging several clip part into a single script.
 * The merged script is then made available to the encoder.
 * 
 * @author Vedran Matic
 *
 */
public final class MergedScript {
	
	private final List<AvsScriptCommand> scriptCommands;
	private final long encodedFrameCount;

	/**
	 * Create a new instance of a merged script.
	 * 
	 * @param scriptCommands Merged commands from the input clips
	 * @param encodedFrameCount Total frame count for the merged script
	 */
	public MergedScript(final List<AvsScriptCommand> scriptCommands, final long encodedFrameCount) {
		this.scriptCommands = scriptCommands;
		this.encodedFrameCount = encodedFrameCount;
	}
	
	public long getEncodedFrameCount() {
		return encodedFrameCount;
	}
	
	public List<AvsScriptCommand> getCommands() {
		return scriptCommands;
	}
}
