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

import java.nio.file.Path;

/**
 * A bean containing the info needed for encoding of a segment.
 * 
 * @author Vedran Matic
 *
 */
public final class AvsSegment {

	private final String command;
	private final long frameCount;
	
	private Path avsFilePath;
	private Path x264FilePath;
	
	public AvsSegment(final String command, final Path avsFilePath, final Path x264FilePath, final long frameCount) {
		this.command = command;
		this.frameCount = frameCount;
		this.avsFilePath = avsFilePath;
		this.x264FilePath = x264FilePath;
	}
	
	public final String getCommand() {
		return command;
	}
	
	public final long getFrameCount() {
		return frameCount;
	}

	public final Path getAvsFilePath() {
		return avsFilePath;
	}

	public final Path getX264FilePath() {
		return x264FilePath;
	}	
}
