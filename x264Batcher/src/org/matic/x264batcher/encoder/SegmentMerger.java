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

import org.matic.x264batcher.exception.EncoderException;
import org.matic.x264batcher.model.AvsSegment;
import org.matic.x264batcher.model.EncoderJobParameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Merger of encoded segments. The resulting file is a playable x264 video file.
 * The merging itself is performed by the mkvmerge.exe.
 * 
 * @author Vedran Matic
 *
 */
final class SegmentMerger implements Runnable {
	
	private volatile Exception error = null;
	
	private final List<AvsSegment> segments;
	
	private final EncoderJobParameters jobParameters;
	private final String mkvMergePath;
	private final String command;
	
	/**
	 * Create a new instance of segment merger.
	 * 
	 * @param segments Segments to be merged
	 * @param jobParameters Parent encoding job parameters
	 * @param mkvMergePath Path to the mkvmerge executable
	 */
	SegmentMerger(final List<AvsSegment> segments, final EncoderJobParameters jobParameters,
				  final String mkvMergePath) {
		this.segments = segments;
		this.mkvMergePath = mkvMergePath;
		this.jobParameters = jobParameters;

		command = buildSegmentMergeCommand();
	}

	/**
	 * Get any errors that occurred during the merging.
	 * 
	 * @return Possible merge error or null if everything went fine
	 */
	public Exception getError() {
		return error;
	}
	
	/**
	 * Get the mkvmerge command that will be used for merging. 
	 * 
	 * @return The mkvmerge command
	 */
	public String getCommand() {
		return command;
	}

	@Override
	public void run() {	
		
		final String[] commandTokens = command.trim().split(" ");
		
		System.out.println("Merger params: " + Arrays.toString(commandTokens));
		
		final ProcessBuilder builder = new ProcessBuilder(commandTokens);
		builder.redirectErrorStream(true);

		try {
			final Process process = builder.start();
			final BufferedReader is = new BufferedReader(new InputStreamReader(process.getInputStream()));
			
			//Drain the process output buffers to avoid deadlock
			while(is.readLine() != null);
			
			System.out.println("SegmentMerger: Waiting for the process to complete");
			process.waitFor();
		} catch (final InterruptedException | IOException e) {
			error = new EncoderException("Failed to merge files: " + e.getMessage());
			System.out.println(e);
		} finally {
			System.out.println("SegmentMerger: process has completed");
			//Delete temporary files if needed 
			if(jobParameters.isDeleteTemporaryFiles()) {
				segments.forEach(p -> {
					p.getAvsFilePath().toFile().delete();
					p.getX264FilePath().toFile().delete();
				});
			}
		}
	}	
	
	private String buildSegmentMergeCommand() {		
		final Path mergedFilePath = Paths.get(jobParameters.getJobOutputPath(),
					jobParameters.getName() + ".mkv");
		final StringBuilder command = new StringBuilder();
		command.append(mkvMergePath)
			.append(" -o ")
			.append(mergedFilePath.toString())
			.append(" ")
			.append(segments.stream().map(p -> p.getX264FilePath().toString())
				.collect(Collectors.joining(" + ")));
		
		return command.toString();
	}
}