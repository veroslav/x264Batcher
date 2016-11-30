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
package org.matic.x264batcher.encoder.log;

/**
 * A logger interface to be used in GUI components.
 * 
 * @author Vedran Matic
 *
 */
public interface EncoderLogger {

	/**
	 * Remove all log entries from the logger.
	 */
	void clear();
	
	/**
	 * Append a log entry content to the logger.
	 * 
	 * @param severity The severity of the log entry
	 * @param content The log text content to be appended
	 */
	void log(LogEntry.Severity severity, String content);
	
	/**
	 * Show only log entries that match target severity level.
	 * 
	 * @param severity Severity level to filter on
	 */
	void filter(LogEntry.Severity severity);
	
	/**
	 * Show only log entries that match target severity level and contain target search text.
	 * 
	 * @param severity Severity level to filter on
	 * @param filterText Target text that the log entry must contain 
	 */
	void filter(LogEntry.Severity severity, String filterText);
}