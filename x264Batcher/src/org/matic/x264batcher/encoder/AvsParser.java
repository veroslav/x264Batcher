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
package org.matic.x264batcher.encoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.matic.x264batcher.gui.log.EncoderLogger;
import org.matic.x264batcher.gui.log.LogEntry;
import org.matic.x264batcher.exception.EncoderException;
import org.matic.x264batcher.model.AvsInputFile;
import org.matic.x264batcher.model.AvsScriptCommand;
import org.matic.x264batcher.parser.D2VParser;
import org.matic.x264batcher.parser.DgiParser;
import org.matic.x264batcher.parser.IndexedFileParser;
import org.matic.x264batcher.parser.ParsedIndexedFile;

/**
 * A utility for parsing of AVS script files.
 * 
 * @author Vedran Matic
 *
 */
public final class AvsParser {

	/**
	 * Parse a list of AVS scripts to an internal representation to be used by the encoder.
	 * 
	 * @param inputAvsPaths A list of file paths to the AVS scripts to be parsed
	 * @param logger Log the parser progress output to this logger 
	 * @return Internal representation of parsed AVS scripts
	 * @throws IOException If any error occurs during file parsing
	 */
	public static List<AvsInputFile> parseInputAvs(final List<String> inputAvsPaths, final EncoderLogger logger) throws IOException, EncoderException {
		final List<AvsInputFile> avsInputFiles = new ArrayList<>();
		for(final String avsPath : inputAvsPaths) {
			final List<String> avsFileLines = Files.readAllLines(Paths.get(avsPath));
			final List<AvsScriptCommand> scriptCommands = new ArrayList<>();			
			
			ParsedIndexedFile parsedIndexedFile = null;
			long estimatedFrameCount = -1;
			
			for(final String line : avsFileLines) {
				if(processAvsCommandLine(line, scriptCommands)) {
					continue;
				}
				//Check for indexed source calls
				else  {					
					//Check whether it is a reference to a DGSource					
					if(line.contains(AvsScriptCommand.DGSOURCE_IDENTIFIER) && line.contains(DgiParser.DGI_FILE_EXTENSION)) {
						final IndexedFileParser indexedFileParser = new DgiParser();
						parsedIndexedFile = indexedFileParser.parse(line.substring(
								line.indexOf(AvsScriptCommand.DGSOURCE_IDENTIFIER) + 
								AvsScriptCommand.DGSOURCE_IDENTIFIER.length(), line.lastIndexOf("\"")));
						scriptCommands.add(new AvsScriptCommand(AvsScriptCommand.DGSOURCE_IDENTIFIER, line));
					}
					//Check whether it is a reference to a DGDecode
					else if(line.contains(AvsScriptCommand.DGDECODE_IDENTIFIER) && line.contains(D2VParser.D2V_FILE_EXTENSION)) {
						final IndexedFileParser indexedFileParser = new D2VParser();
						parsedIndexedFile = indexedFileParser.parse(
								line.substring(line.indexOf(AvsScriptCommand.DGDECODE_IDENTIFIER) + 
										AvsScriptCommand.DGDECODE_IDENTIFIER.length(), line.lastIndexOf("\"")));	
						scriptCommands.add(new AvsScriptCommand(AvsScriptCommand.DGDECODE_IDENTIFIER, line));
					}				
					if(parsedIndexedFile != null) {
						estimatedFrameCount = parsedIndexedFile.getFrameCount();
						continue;
					}
				}
				scriptCommands.add(new AvsScriptCommand(AvsScriptCommand.GENERIC, line));
			}
			
			logger.log(LogEntry.Severity.INFO, "Estimated frame count: " + estimatedFrameCount + " for AVS = [" + avsPath  + " ]");
			
			final Optional<AvsScriptCommand> explicitTrimCommand = scriptCommands.stream().filter(
					c -> AvsScriptCommand.TRIM.equals(c.getIdentifier())).findAny();
			
			if(!explicitTrimCommand.isPresent() && estimatedFrameCount > -1) {
				//Add estimated explicit Trim() value (assuming whole file should be encoded) 
				final AvsScriptCommand trimCommand = new AvsScriptCommand(AvsScriptCommand.TRIM, AvsScriptCommand.TRIM + "0,"
						+ estimatedFrameCount + ")");
				
				final OptionalInt minInsertionIndex = scriptCommands.stream().filter(c ->
					c.getCommand().contains(AvsScriptCommand.DGDECODE_IDENTIFIER) ||
					c.getCommand().contains(AvsScriptCommand.DGSOURCE_IDENTIFIER) ||
					c.getCommand().contains(AvsScriptCommand.COLOR_MATRIX)).mapToInt(scriptCommands::indexOf).max();
			
				final OptionalInt maxInsertionIndex = scriptCommands.stream().filter(c ->
					c.getCommand().contains(AvsScriptCommand.QTGMC) ||
					c.getCommand().contains(AvsScriptCommand.TRIM)).mapToInt(scriptCommands::indexOf).min();
			
				scriptCommands.add(Math.max(minInsertionIndex.getAsInt(), maxInsertionIndex.getAsInt()), trimCommand);			
			} else if(estimatedFrameCount == -1) {
				throw new EncoderException("Unable to determine clip dimensions");
			}
			
			avsInputFiles.add(new AvsInputFile(Paths.get(avsPath), parsedIndexedFile, scriptCommands));
		}
		
		return avsInputFiles;
	}
	
	private static boolean processAvsCommandLine(final String line, final List<AvsScriptCommand> avsCommands) {
		//TODO: Refactor to an array of AvsScript being iterated through for command line matches
		//Check for and ignore comment lines (#) and MeGUI directives
		if(line.startsWith(AvsScriptCommand.COMMENT) || line.trim().equals("")
				|| line.contains("MeGUI")) {
			return true;
		}
		//Check for explicit Trim() function call
		if(line.contains(AvsScriptCommand.TRIM)) {					
			avsCommands.add(new AvsScriptCommand(AvsScriptCommand.TRIM, line));
			return true;
		}
		//Check for plug-in loading function call
		else if(line.contains(AvsScriptCommand.LOAD_PLUGIN)) {
			avsCommands.add(new AvsScriptCommand(AvsScriptCommand.LOAD_PLUGIN, line));
			return true;
		}
		//Check for QTGMC() call
		else if(line.contains(AvsScriptCommand.QTGMC)) {
			avsCommands.add(new AvsScriptCommand(AvsScriptCommand.QTGMC, line));
			return true;
		}
		//Check for SelectEven() call
		else if(line.contains(AvsScriptCommand.SELECT_EVEN)) {
			avsCommands.add(new AvsScriptCommand(AvsScriptCommand.SELECT_EVEN, line));
			return true;
		}
		//Check for crop() call
		else if(line.contains(AvsScriptCommand.CROP)) {
			avsCommands.add(new AvsScriptCommand(AvsScriptCommand.CROP, line));
			return true;
		}
		//Check for ColorMatrix() call
		else if(line.contains(AvsScriptCommand.COLOR_MATRIX)) {
			avsCommands.add(new AvsScriptCommand(AvsScriptCommand.COLOR_MATRIX, line));
			return true;
		}
		//Check for Undot() call
		else if(line.contains(AvsScriptCommand.UNDOT)) {
			avsCommands.add(new AvsScriptCommand(AvsScriptCommand.UNDOT, line));
			return true;
		}
		//Check for Tweak() call
		else if(line.contains(AvsScriptCommand.TWEAK)) {
			avsCommands.add(new AvsScriptCommand(AvsScriptCommand.TWEAK, line));
			return true;
		}
		//Check for Spline36Resize() call
		else if(line.contains(AvsScriptCommand.RESIZE)) {
			avsCommands.add(new AvsScriptCommand(AvsScriptCommand.RESIZE, line));
			return true;
		}
		return false;
	}
}
