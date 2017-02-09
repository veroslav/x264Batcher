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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.matic.x264batcher.gui.log.EncoderLogger;
import org.matic.x264batcher.gui.model.QueuedJob;
import org.matic.x264batcher.model.AvsInputFile;
import org.matic.x264batcher.model.AvsScriptCommand;
import org.matic.x264batcher.model.AvsSegment;
import org.matic.x264batcher.model.ClipDimension;
import org.matic.x264batcher.model.EncoderParameters;
import org.matic.x264batcher.model.MergedScript;

/**
 * Builder of clip segments from the AVS scripts that are part of an encoding job.
 * The entire length (in frames) of the included AVS scripts is evenly divided into
 * segments of approximately same length (in frames). A segment can contain multiple
 * AVS scripts (or parts of these) if the segment length is greater than the length
 * of included clips. 
 * 
 * @author Vedran Matic
 *
 */
final class SegmentBuilder {
	
	private static final String SEGMENT_NAME_PREFIX = "_seg_";
	
	private final EncoderParameters encoderParameters;
	private final QueuedJob queuedJob;
	private final EncoderLogger logger;
	
	/**
	 * Create a new instance of segment builder.
	 * 
	 * @param encoderParameters x264 command parameters used for this segment's encoding
	 * @param queuedJob Encoding job to which this segment belongs to
	 * @param logger Build progress is logged to this logger
	 */
	SegmentBuilder(final EncoderParameters encoderParameters,
				   final QueuedJob queuedJob, final EncoderLogger logger) {
		this.encoderParameters = encoderParameters;
		this.queuedJob = queuedJob;
		this.logger = logger;
	}
	
	/**
	 * Build all segments that cover and divide the whole clip into approximately even parts.
	 * 
	 * @return Built segments
	 * @throws IOException If any error occurs while the segments are written to the disk
	 */
	List<AvsSegment> buildSegments() throws IOException {
		final List<AvsInputFile> inputFiles = queuedJob.getInputAvsFiles();
		final long totalFrames = inputFiles.stream().mapToLong(AvsInputFile::getFrameCount).sum();
		final ClipDimension clipDimension = queuedJob.getTargetClipDimension();
		return buildSegments(inputFiles, totalFrames, clipDimension);
	}
	
	private List<AvsSegment> buildSegments(final List<AvsInputFile> inputFiles, final long totalFrames,
			final ClipDimension clipDimension) throws IOException {
		
		final List<AvsSegment> builtSegments = new ArrayList<>();
		final long segmentLength = (long)Math.ceil(((double)totalFrames) / encoderParameters.getEncoderJobsLimit());
		final ScriptMerger scriptMerger = new ScriptMerger(clipDimension);
		
		int currentAvsFileIndex = 0;
		long currentSegmentOffset = 0;
		long currentAvsFileOffset = 0;
		long currentFrame = 0;
		 
		while(currentFrame < totalFrames) {
			final AvsInputFile currentInputAvsFile = inputFiles.get(currentAvsFileIndex);
			final long loopAvsFileOffset = currentInputAvsFile.getClipStart() + currentAvsFileOffset;
			final long clipLengthLeft = currentInputAvsFile.getClipEnd() - loopAvsFileOffset + 1;
			final long segmentLengthLeft = segmentLength - currentSegmentOffset;
			
			if((segmentLengthLeft >= clipLengthLeft) && 
					(currentFrame + clipLengthLeft >= totalFrames)) {
				//We are almost done, this is the last segment
				final long framesLeft = totalFrames - currentFrame;
				scriptMerger.addScript(currentInputAvsFile, loopAvsFileOffset,
						loopAvsFileOffset + framesLeft - 1);
				final MergedScript mergedScript = scriptMerger.merge();
				final AvsSegment avsSegment = buildAvsSegment(builtSegments.size(), mergedScript);
				builtSegments.add(avsSegment);
				break;
			}
			
			//Check if what's left of this AVS covers the rest of the segment
			if(clipLengthLeft >= segmentLengthLeft) {
				//It does, cut it so that it covers the segment precisely, and create the segment	
				final long avsFrameEnd = loopAvsFileOffset + segmentLengthLeft - 1;
				scriptMerger.addScript(currentInputAvsFile, loopAvsFileOffset, avsFrameEnd);
				final MergedScript mergedScript = scriptMerger.merge();
				final AvsSegment avsSegment = buildAvsSegment(builtSegments.size(), mergedScript);
				builtSegments.add(avsSegment);
				currentFrame += (avsFrameEnd - loopAvsFileOffset + 1);
				
				//If the segment length is equal to this AVS file length, move to next AVS file
				if(segmentLengthLeft == clipLengthLeft) {
					++currentAvsFileIndex;
				}
				else {
					currentAvsFileOffset += segmentLengthLeft;					
				}
				currentSegmentOffset = 0;
			}
			else {
				//Too short clip, get more frames from the next AVS file. Modify input AVS
				scriptMerger.addScript(currentInputAvsFile, loopAvsFileOffset,
						currentInputAvsFile.getClipEnd());
				currentSegmentOffset += clipLengthLeft;
				currentFrame += clipLengthLeft;
				currentAvsFileOffset = 0;
				++currentAvsFileIndex;
			}
		}
		
		return builtSegments;
	}
	
	private AvsSegment buildAvsSegment(final long segmentId, final MergedScript mergedScript) throws IOException {
		final String jobName = queuedJob.getName();
		final Path workDir = Paths.get(queuedJob.getOutputPath());
		
		final StringBuilder avsSegmentName = new StringBuilder();
		avsSegmentName.append(jobName);
		avsSegmentName.append(SEGMENT_NAME_PREFIX);
		avsSegmentName.append(segmentId);
		avsSegmentName.append(".avs");

		final Path avsSegmentPath = Paths.get(workDir.toString(), avsSegmentName.toString());
		avsSegmentPath.toFile().createNewFile();
		
		Files.write(avsSegmentPath, mergedScript.getCommands().stream().map(
				AvsScriptCommand::getCommand).collect(Collectors.toList()));
				
		final StringBuilder x264SegmentName = new StringBuilder();
		x264SegmentName.append(jobName);
		x264SegmentName.append(SEGMENT_NAME_PREFIX);
		x264SegmentName.append(segmentId);
		x264SegmentName.append(".264");
		
		final Path x264SegmentPath = Paths.get(workDir.toString(), x264SegmentName.toString());
		
		final StringBuilder command = new StringBuilder();
		command.append(encoderParameters.getX264ExecutablePath())
			.append(" ")
			.append(queuedJob.getEncoderPreset().getCommand())			
			.append(" --stitchable --sar ")
			.append(queuedJob.getOutputSar())
			.append(" --output ")
			.append(x264SegmentPath)
			.append(" ")
			.append(avsSegmentPath.toString());		
		
		return new AvsSegment(command.toString(), avsSegmentPath, x264SegmentPath, mergedScript.getEncodedFrameCount());
	}
}