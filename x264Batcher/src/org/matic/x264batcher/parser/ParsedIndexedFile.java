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
package org.matic.x264batcher.parser;

/**
 * Extracted information from a clip's index file.
 * 
 * @author Vedran Matic
 *
 */
public final class ParsedIndexedFile {
	
	private final int width;
	private final int height;
	private final long frameCount;

	/**
	 * Create a new instance of the parsed index file.
	 * 
	 * @param width Clip's width
	 * @param height Clip's height
	 * @param frameCount Clip length (in frames)
	 */
	ParsedIndexedFile(final int width, final int height, final long frameCount) {
		this.width = width;
		this.height = height;
		this.frameCount = frameCount;
	}

	public long getFrameCount() {
		return frameCount;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	@Override
	public String toString() {
		return "ParsedIndexedFileResult [width=" + width + ", height=" + height + ", frameCount=" + frameCount + "]";
	}		
}
