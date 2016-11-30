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
import java.util.List;

import org.matic.x264batcher.parser.ParsedIndexedFile;

/**
 * A representation of an AVS file that is part of an encoder job.
 * 
 * @author Vedran Matic
 *
 */
public final class AvsInputFile {
		
	private final List<AvsScriptCommand> scriptCommands;
	private final Path filePath;
	
	private final long clipStart;
	private final long clipEnd;
	
	private final ClipDimension clipDimension;
	private final boolean usingDeinterlacing;
	
	private final ParsedIndexedFile parsedIndexedFile;

	/**
	 * Create a new instance of an AVS file representation.
	 * 
	 * @param filePath Path to the AVS file on the disk
	 * @param parsedIndexedFile Properties of the indexed (.dgi/.d2v) file generated for this AVS file
	 * @param scriptCommands Contents of the input AVS file 
	 */
	public AvsInputFile(final Path filePath, final ParsedIndexedFile parsedIndexedFile,
			final List<AvsScriptCommand> scriptCommands) {
		this.filePath = filePath;
		this.scriptCommands = scriptCommands;
		this.parsedIndexedFile = parsedIndexedFile;
		
		final AvsScriptCommand trimCommand = this.scriptCommands.stream().filter(
				c -> AvsScriptCommand.TRIM.equals(c.getIdentifier())).findAny().get(); 
		final List<String> trimIndices = trimCommand.getArguments();
		
		clipStart = Long.parseLong(trimIndices.get(0));
		clipEnd = Long.parseLong(trimIndices.get(1));	
		
		final AvsScriptCommand cropCommand = scriptCommands.stream().filter(
				c -> AvsScriptCommand.CROP.equals(c.getIdentifier())).findFirst().get();
		final List<String> cropArguments = cropCommand.getArguments();
		
		final int clipWidth = parsedIndexedFile.getWidth() - (Math.abs(Integer.parseInt(cropArguments.get(0))) 
				+ Math.abs(Integer.parseInt(cropArguments.get(2))));
		final int clipHeight = parsedIndexedFile.getHeight() - (Math.abs(Integer.parseInt(cropArguments.get(1))) 
				+ Math.abs(Integer.parseInt(cropArguments.get(3))));
		
		this.clipDimension = new ClipDimension(clipWidth, clipHeight);
		
		final AvsScriptCommand qtgmcCommand = scriptCommands.stream().filter(
				c -> AvsScriptCommand.QTGMC.equals(c.getIdentifier())).findAny().orElse(null);
		final AvsScriptCommand selectEvenCommand = scriptCommands.stream().filter(
				c -> AvsScriptCommand.SELECT_EVEN.equals(c.getIdentifier())).findAny().orElse(null);
		
		usingDeinterlacing = qtgmcCommand != null && selectEvenCommand == null;
	}
	
	public ParsedIndexedFile getIndexedFile() {
		return parsedIndexedFile;
	}
	
	public List<AvsScriptCommand> getCommands() {
		return scriptCommands;
	}
	
	public boolean isUsingDeinterlacing() {
		return usingDeinterlacing;
	}
	
	public ClipDimension getClipDimension() {
		return clipDimension;
	}
	
	public long getClipStart() {
		return clipStart;
	}
	
	public long getClipEnd() {
		return clipEnd;
	}
	
	public Path getPath() {
		return filePath;
	}
	
	public String getName() {
		return filePath.getFileName().toString();
	}
	
	public long getFrameCount() {
		return clipEnd - clipStart + 1;
	}
	
	@Override
	public String toString() {
		return getName();
	}
}