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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Parses and stores the information extracted from a clip's index file.
 * This is mostly useful for obtaining information about a clip's
 * resolution and frame count. This implementation deals with
 * DGDecode indexed files.
 * 
 * @author Vedran Matic
 *
 */
public final class D2VParser implements IndexedFileParser {
	
	public static final String D2V_FILE_EXTENSION = ".d2v";
	
	private static final String FIELD_OPERATION_IDENTIFIER = "Field_Operation=";
	private static final String RESOLUTION_IDENTIFIER = "Picture_Size=";
	
	/**
	 * @see IndexedFileParser#parse(String)
	 */
	@Override
	public final ParsedIndexedFile parse(final String d2vFilePath) throws IOException {
		int[] resolution = {-1, -1};
		int fieldOperation = -1;
		long fieldCount = 0;
		boolean processingFrameFlags = false;
		
		try(final BufferedReader reader = Files.newBufferedReader(Paths.get(d2vFilePath))) {			
			String line;
			while((line = reader.readLine()) != null) {
				if(line.startsWith(RESOLUTION_IDENTIFIER)) {
					resolution = extractResolution(line);
				}
				else if(line.startsWith(FIELD_OPERATION_IDENTIFIER)) {
					fieldOperation = Integer.parseInt(line.substring(FIELD_OPERATION_IDENTIFIER.length(), line.length()));
				}
				else if(line.trim().equals("")) {
					if(fieldOperation != -1) {
						processingFrameFlags = true;
					}
					else if(processingFrameFlags) {
						processingFrameFlags = false;
					}
				}				
				else if(processingFrameFlags) {
					final String[] lineTokens = line.split(" ");
					if(lineTokens.length > 4) {
						fieldCount += processFrameFlags(lineTokens);
					}
				}
			}
		}	
		
		long frameCount = (long)(Math.ceil(fieldCount / 2.0));
		if(fieldOperation == 1) {
			frameCount *= 0.8;
		}
		
		return new ParsedIndexedFile(resolution[0], resolution[1], frameCount - 2);
	}
	
	private long processFrameFlags(final String[] frameFlags) {
		long fieldCount = 0;
		for(int i = 4; i < frameFlags.length; ++i) {
			if(frameFlags[i].length() < 2) {
				continue;
			}
			if(frameFlags[i].contains("0") || frameFlags[i].contains("2")) {
				fieldCount += 2;
			}
			else if(frameFlags[i].contains("1") || frameFlags[i].contains("3")) {
				fieldCount += 3;
			}
		}
		return fieldCount;
	}
	
	private int[] extractResolution(final String resolution) {
		final String[] dimensions = resolution.substring(RESOLUTION_IDENTIFIER.length(),
				resolution.length()).split("x");
		return new int[]{ Integer.parseInt(dimensions[0]), Integer.parseInt(dimensions[1]) };
	}
}
