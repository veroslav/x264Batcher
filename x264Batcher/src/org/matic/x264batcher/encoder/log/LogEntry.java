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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Set;

/**
 * A log entry that contains the timestamp, severity and the log content itself.
 * 
 * @author Vedran Matic
 *
 */
public final class LogEntry {
	public enum Severity {
		ALL, INFO, WARN, ERROR
	}

	private static EnumMap<Severity, String> CSS_STYLES_MAP = new EnumMap<>(Severity.class);

	static {
		CSS_STYLES_MAP.put(Severity.WARN, "-fx-text-fill: orange");
		CSS_STYLES_MAP.put(Severity.INFO, "-fx-text-fill: rgb(0,0,0)");
		CSS_STYLES_MAP.put(Severity.ERROR, "-fx-text-fill: red");
	}

	private static final DateTimeFormatter TIME_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private final Severity severity;
	private final String content;

	private final String time;

	/**
	 * Create a new log entry instance.
	 * 
	 * @param severity The severity of the log event
	 * @param content The log message content
	 */
	public LogEntry(final Severity severity, final String content) {
		this.severity = severity;
		this.content = content;

		time = LocalDateTime.now().format(TIME_FORMATTER);
	}

	public Severity getSeverity() {
		return severity;
	}

	public String getContent() {
		return content;
	}

	public String getStyle() {
		return severity == Severity.ALL? CSS_STYLES_MAP.get(Severity.INFO) : CSS_STYLES_MAP.get(severity);
	}

	public static Set<Severity> getAvailableSeverities() {
		return CSS_STYLES_MAP.keySet();
	}

	@Override
	public String toString() {
		return time + ": [" + severity + "] " + content;
	}
}
