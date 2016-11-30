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

import org.matic.x264batcher.encoder.log.EncoderLogger;
import org.matic.x264batcher.encoder.log.LogEntry.Severity;
import org.matic.x264batcher.exception.EncoderException;
import org.matic.x264batcher.model.SegmentEncoderResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

/**
 * An instance of x264.exe process that is encoding a portion (segment) of an input AVS script.
 * 
 * @author Vedran Matic
 *
 */
final class SegmentEncoder implements Callable<SegmentEncoderResult> {
	
	private final String jobCommand;
	private final EncoderLogger logger;

	private volatile String jobOutput = "";
	private volatile long framesDone = 0;
	
	/**
	 * Create a new instance of the encoder.
	 * 
	 * @param jobCommand The x264.exe command to execute
	 * @param logger Output progress info to this logger
	 */
	SegmentEncoder(final String jobCommand, final EncoderLogger logger) {
		this.jobCommand = jobCommand;
		this.logger = logger;
	}
	
	void setFramesDone(final long framesDone) {
		this.framesDone = framesDone;
	}
	
	long getFramesDone() {
		return framesDone;
	}
	
	public String getCommand() {
		return jobCommand;
	}
	
	String getOutput() {
		return jobOutput;
	}

	@Override
	public SegmentEncoderResult call() {
		
		logger.log(Severity.INFO, "Encoding segment: Command = " + jobCommand);
		
		final String[] commandTokens = jobCommand.trim().split(" ");
		final ProcessBuilder builder = new ProcessBuilder(commandTokens);	
		builder.redirectErrorStream(true);
		
		final Process process;
		try {
			process = builder.start();
		} catch (final IOException ioe) {
			return new SegmentEncoderResult(SegmentEncoderResult.FAILED,
					new EncoderException("Segment command creation failure: " + ioe.getMessage()));
		}
		
		final BufferedReader is = new BufferedReader(new InputStreamReader(process.getInputStream()));		
		String line;
		
		try {
			while(!Thread.currentThread().isInterrupted() && ((line = is.readLine()) != null)) {
				jobOutput = line;
			}
			if(Thread.currentThread().isInterrupted()) {
				Thread.interrupted();
				process.destroyForcibly().waitFor();
				return new SegmentEncoderResult(SegmentEncoderResult.FAILED,
						new EncoderException("Segment encoder was interrupted: command = " + jobCommand));
			}
			final int exitCode = process.waitFor();
			if(exitCode != 0) {
				return new SegmentEncoderResult(SegmentEncoderResult.FAILED,
						new EncoderException("Encoder completed with an error = " + exitCode));
			}
			
		} catch(final InterruptedException | IOException e) {			
			e.printStackTrace();
		}
		
		return new SegmentEncoderResult(SegmentEncoderResult.SUCCESS, null);
	}
}