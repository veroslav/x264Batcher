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
import java.util.Arrays;

import org.matic.x264batcher.utils.Helper;

/**
 * Parses and stores the information extracted from a clip's index file.
 * This is mostly useful for obtaining information about a clip's
 * resolution and frame count. This implementation deals with
 * DGSource indexed files.
 * 
 * @author Vedran Matic
 *
 */
public final class DgiParser implements IndexedFileParser {
	
	public static final String DGI_FILE_EXTENSION = ".dgi";
	
	private static final String RESOLUTION_IDENTIFIER = "SIZ";
	
	/**
	 * @see IndexedFileParser#parse(String) 
	 */
	@Override
	public ParsedIndexedFile parse(final String dgiFilePath) throws IOException {
		int[] resolution = {-1, -1};
		try (final BufferedReader reader = Files.newBufferedReader(Paths.get(dgiFilePath))) {
			String line;
			while((line = reader.readLine()) != null) {
				if(line.startsWith(RESOLUTION_IDENTIFIER) && line.contains("x")) {
					resolution = extractResolution(line);
				}
			}
		}
		return new ParsedIndexedFile(resolution[0], resolution[1], -1);
	}
	
	private int[] extractResolution(final String resolution) {
		return Arrays.stream(resolution.split(" ")).filter(
				Helper::isNumber).mapToInt(Integer::parseInt).toArray();			
	}
}
