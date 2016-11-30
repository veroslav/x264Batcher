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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A representation of a script command in an AVS file.
 * 
 * @author Vedran Matic
 *
 */
public final class AvsScriptCommand {
	
	public static final String COLOR_MATRIX = "ColorMatrix(";
	public static final String LOAD_PLUGIN = "LoadPlugin(";
	public static final String SELECT_EVEN = "SelectEven(";
	public static final String RESIZE = "Spline36Resize(";
	public static final String TRIM = "Trim(";
	public static final String CROP = "crop(";
	public static final String COMMENT = "#";
	public static final String QTGMC = "QTGMC(";
	public static final String UNDOT = "Undot(";
	public static final String TWEAK = "Tweak(";
	
	public static final String DGDECODE_IDENTIFIER = "DGDecode_mpeg2source(\"";
	public static final String DGSOURCE_IDENTIFIER = "DGSource(\"";
	
	public static final String GENERIC = "";

	private final String identifier;
	private String command;
	
	/**
	 * Create a new command instance representation.
	 * 
	 * @param identifier The type of the command (such as LoadPlugin() or Undot())
	 * @param command The complete command line
	 */
	public AvsScriptCommand(final String identifier, final String command) {
		this.identifier = identifier;
		this.command = command;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getCommand() {
		return command;
	}
	
	public void setCommand(final String command) {
		this.command = command;
	}
	
	/**
	 * Get the command's argument, if there are any.
	 * 
	 * @return A list of command arguments, or an empty list if there are none
	 */
	List<String> getArguments() {
		final int argStart = command.indexOf("(");
		final int argEnd = command.indexOf(")");
		
		if(argStart == -1 || argEnd == -1) {
			return Collections.emptyList();
		}
		
		final String[] args = command.substring(argStart + 1 , argEnd).split(",");
		return Arrays.stream(args).map(String::trim).collect(Collectors.toList());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((command == null) ? 0 : command.hashCode());
		result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
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
		AvsScriptCommand other = (AvsScriptCommand) obj;
		if (command == null) {
			if (other.command != null)
				return false;
		} else if (!command.equals(other.command))
			return false;
		if (identifier == null) {
			if (other.identifier != null)
				return false;
		} else if (!identifier.equals(other.identifier))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return command;
	}	
	
}