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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * A simple file logger that writes contents of ListView log to a file.
 *
 * @author Vedran Matic
 */
public final class FileLogger {

    private BufferedWriter logWriter;

    public void openForWriting(final File logFile) throws IOException {
         logWriter = Files.newBufferedWriter(logFile.toPath(), StandardCharsets.UTF_8);
    }

    public void write(final LogEntry logEntry) throws IOException {
        if(logWriter != null) {
            logWriter.write(logEntry.getContent());
            logWriter.newLine();
        }
    }

    public void closeForWriting() throws IOException {
        if(logWriter != null) {
            logWriter.close();
        }
    }
}