/*
* This file is part of x264Batcher, an x264 encoder multiplier written in JavaFX.
* Copyright (C) 2016-2017 Vedran Matic
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
package org.matic.x264batcher.gui.log;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ListView;

/**
 * A logger that displays the info about an encoding's progress in a ListView.
 * 
 * @author Vedran Matic
 *
 */
public final class ListViewEncoderLogger implements EncoderLogger {
	
	private final ObservableList<LogEntry> logEntries = FXCollections.observableArrayList();
	private final FilteredList<LogEntry> filteredLogEntries;
	private final ListView<LogEntry> loggerView;

	/**
	 * Create a new instance of the logger view.
	 * 
	 * @param loggerView The ListView to which to append the log entries
	 */
	public ListViewEncoderLogger(final ListView<LogEntry> loggerView) {
		this.loggerView = loggerView;
		
		filteredLogEntries = new FilteredList<>(logEntries, entry -> true);
		this.loggerView.setItems(filteredLogEntries);
	}

	/**
	 * @see EncoderLogger#clear()
	 */
	@Override
	public void clear() {
		logEntries.clear();
	}

	/**
	 * @see EncoderLogger#filter(org.matic.x264batcher.gui.log.LogEntry.Severity)
	 */
	@Override
	public void filter(final LogEntry.Severity severity) {
		if(severity == LogEntry.Severity.ALL) {
            filteredLogEntries.setPredicate(entry -> true);
        }
        else {
            filteredLogEntries.setPredicate(entry -> entry.getSeverity() == severity);
        }
	}

	/**
	 * @see EncoderLogger#filter(org.matic.x264batcher.gui.log.LogEntry.Severity, String)
	 */
	@Override
	public void filter(final LogEntry.Severity severity, final String filterText) {
		filteredLogEntries.setPredicate(entry ->
			entry.getContent().contains(filterText) && (severity == LogEntry.Severity.ALL?
				true : entry.getSeverity() == severity));
	}

	/**
	 * @see EncoderLogger#log(org.matic.x264batcher.gui.log.LogEntry.Severity, String)
	 */
	@Override
	public void log(final LogEntry.Severity severity, final String content) {
		Platform.runLater(() -> {
			logEntries.add(new LogEntry(severity, content));
		});
	}
}
