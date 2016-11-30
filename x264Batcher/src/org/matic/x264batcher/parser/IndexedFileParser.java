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

import java.io.IOException;

/**
 * Classes implementing this interface have ability to parse
 * and extract useful info from a clip's index file.
 * 
 * @author Vedran Matic
 *
 */
public interface IndexedFileParser {

	/**
	 * Given a clip's index file at a certain location, parse it
	 * and return the extracted info.
	 * 
	 * @param indexedFilePath Path to the index file
	 * @return Extracted info from the index file
	 * @throws IOException If any error occurs during extraction
	 */
	ParsedIndexedFile parse(String indexedFilePath) throws IOException;
}