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

import org.matic.x264batcher.model.AvsInputFile;
import org.matic.x264batcher.model.AvsScriptCommand;
import org.matic.x264batcher.model.ClipDimension;
import org.matic.x264batcher.model.MergedScript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Collects parts of AVS scripts and creates a single, merged, AVS script from these parts.
 * 
 * @author Vedran Matic
 *
 */
final class ScriptMerger {

	private final List<AvsInputFile> scriptFiles = new ArrayList<>();
	
	private final ClipDimension clipDimension;
	
	/**
	 * Create a new instance of the script merger.
	 * 
	 * @param clipDimension Clip dimension to use for merged AVS script. If clip dimension for
	 * any of the clips that are being merged differs from this dimension, it will be resized
	 * so that it matches it. 
	 */
	ScriptMerger(final ClipDimension clipDimension) {
		this.clipDimension = clipDimension;
	}
	
	/**
	 * Append a part of an AVS script to the list of scripts to be merged into a single AVS script.
	 * 
	 * @param scriptFile AVS script to append
	 * @param frameStart Clip start frame offset
	 * @param frameEnd Clip end frame offset
	 */
	void addScript(final AvsInputFile scriptFile, final long frameStart, final long frameEnd) {
		//Modify the script so that we only use the part between frameStart and frameEnd (inclusive)
		final AvsInputFile modifiedInputFile = modifyTrimInterval(scriptFile, frameStart, frameEnd);
		
		scriptFiles.add(modifiedInputFile);
	}

	/**
	 * Joins all of the previously added AVS scripts into a single merged AVS script. 
	 * 
	 * @return The merged script
	 */
	MergedScript merge() {
		final List<AvsScriptCommand> mergedScriptCommands = new ArrayList<>();
		
		long encodedFrameCount = 0; 
		final String[] clipNames = new String[scriptFiles.size()];	
		for(int i = 0; i < clipNames.length; ++i) {
			final String clipName = "clip_" + i; 
			clipNames[i] = clipName;
			final AvsInputFile scriptFile = scriptFiles.get(i);
			final List<AvsScriptCommand> deepCopyCommands = deepCopyCommands(scriptFile.getCommands());
			final List<AvsScriptCommand> resizeCommands = deepCopyCommands.stream().filter(
					c -> AvsScriptCommand.RESIZE.equals(c.getIdentifier())).collect(Collectors.toList());
			if(resizeCommands.isEmpty() && !scriptFile.getClipDimension().equals(clipDimension)) {
				//Clip needs resizing
				insertExplicitResizing(deepCopyCommands, clipNames[i]);
			}
			else if(!resizeCommands.isEmpty()) {
				//Clip already has explicit resizing, adjust clip name only
				prefixExistingResizeCommands(clipName, resizeCommands);
			}
			final List<AvsScriptCommand> modifiedCommands = appendClipNamePrefix(clipNames[i], deepCopyCommands);
			insertScriptCommands(mergedScriptCommands, modifiedCommands);
			final long clipFrameCount = scriptFile.getFrameCount();
			encodedFrameCount += scriptFile.isUsingDeinterlacing()? 2 * clipFrameCount : clipFrameCount; 
		}
		
		final StringBuilder joinClipsCommand = new StringBuilder();
		joinClipsCommand.append("return ");		
		joinClipsCommand.append(Arrays.stream(clipNames).collect(Collectors.joining(" ++ ")));
		
		mergedScriptCommands.add(new AvsScriptCommand(AvsScriptCommand.GENERIC, joinClipsCommand.toString()));
		
		//Prepare the merger for next script
		scriptFiles.clear();
		
		//Remove duplicate LoadPlugin directives, if any, before returning the result
		final List<AvsScriptCommand> loadPluginCommands = mergedScriptCommands.stream().filter(
				c -> AvsScriptCommand.LOAD_PLUGIN.equals(c.getIdentifier())).collect(Collectors.toList());
		
		mergedScriptCommands.removeAll(loadPluginCommands);
		mergedScriptCommands.addAll(0, loadPluginCommands.stream().distinct().collect(Collectors.toList()));
		
		return new MergedScript(mergedScriptCommands, encodedFrameCount);
	}

	private void prefixExistingResizeCommands(final String clipName, final List<AvsScriptCommand> resizeCommands) {
		resizeCommands.forEach(c -> {
			final StringBuilder commandBuilder = new StringBuilder(c.getCommand());
			commandBuilder.insert(0, ".");
			commandBuilder.insert(0, clipName);
			commandBuilder.insert(0, "=");
			commandBuilder.insert(0, clipName);
			final int widthIndex = commandBuilder.indexOf("width");
			if(widthIndex != -1) {
				commandBuilder.insert(widthIndex, ".");
				commandBuilder.insert(widthIndex, clipName);
			}
			final int heightIndex = commandBuilder.indexOf("height");
			if(heightIndex != -1) {
				commandBuilder.insert(heightIndex, ".");
				commandBuilder.insert(heightIndex, clipName);
			}
			c.setCommand(commandBuilder.toString());
		});
	}	
	
	private List<AvsScriptCommand> deepCopyCommands(final List<AvsScriptCommand> commands) {
		return commands.stream().map(c -> new AvsScriptCommand(c.getIdentifier(), c.getCommand()))
				.collect(Collectors.toList());
	}
	
	private AvsInputFile modifyTrimInterval(final AvsInputFile scriptFile, final long frameStart, final long frameEnd) {		
		final List<AvsScriptCommand> scriptCommands = scriptFile.getCommands();
		final AvsScriptCommand trimCommand = scriptCommands.stream().filter(
				c -> AvsScriptCommand.TRIM.equals(c.getIdentifier())).findFirst().get();
		
		final AvsScriptCommand modifiedTrimCommand = new AvsScriptCommand(AvsScriptCommand.TRIM,
				AvsScriptCommand.TRIM + frameStart + "," + frameEnd + ")");
		
		final int trimCommandIndex = scriptCommands.indexOf(trimCommand);
		scriptCommands.set(trimCommandIndex, modifiedTrimCommand);
		
		return new AvsInputFile(
				scriptFile.getPath(), scriptFile.getIndexedFile(), scriptCommands);
	}
	
	private void insertExplicitResizing(final List<AvsScriptCommand> scriptFileCommands, final String clipName) {
		final Optional<AvsScriptCommand> qtgmcCommand = scriptFileCommands.stream().filter(
				c -> AvsScriptCommand.QTGMC.equals(c.getIdentifier())).findFirst();
		
		if(qtgmcCommand.isPresent()) {
			//We can divide resizing and make the deinterlacing more efficient, let's do that
			final int qtgmcCommandIndex = scriptFileCommands.indexOf(qtgmcCommand.get());
			
			final AvsScriptCommand resizeWidthCommand = new AvsScriptCommand(
					AvsScriptCommand.RESIZE, clipName + "=" + clipName + "." + AvsScriptCommand.RESIZE + clipDimension.getWidth() 
					+ "," + clipName + ".height)");
			final AvsScriptCommand resizeHeightCommand = new AvsScriptCommand(
					AvsScriptCommand.RESIZE, clipName + "=" + clipName + "." + AvsScriptCommand.RESIZE + clipName 
					+ ".width," + clipDimension.getHeight() + ")");
			
			//Insert height resizing after the deinterlacing
			scriptFileCommands.add(qtgmcCommandIndex + 1, resizeHeightCommand);
			
			//Insert width resizing before the deinterlacing
			scriptFileCommands.add(qtgmcCommandIndex, resizeWidthCommand);
			
		}
		else {
			//Clip will not be deinterlaced, we add a one line resizing command at the clip's end
			final AvsScriptCommand resizeCommand = new AvsScriptCommand(AvsScriptCommand.RESIZE,
					AvsScriptCommand.RESIZE + clipDimension.getWidth() 
					+ "," + clipDimension.getHeight() + ")");
			scriptFileCommands.add(resizeCommand);
		}
	}
	
	private void insertScriptCommands(final List<AvsScriptCommand> mergedScriptCommands,
			final List<AvsScriptCommand> modifiedCommands) {
		for(final AvsScriptCommand command : modifiedCommands) {
			if(AvsScriptCommand.LOAD_PLUGIN.equals(command.getIdentifier())) {
				//Insert LoadPlugin commands at the top of the script
				mergedScriptCommands.add(0, command);
			}
			else {
				//All other commands are appended at the end of the script
				mergedScriptCommands.add(command);
			}
		}
	}
	
	private List<AvsScriptCommand> appendClipNamePrefix(final String prefix, final List<AvsScriptCommand> scriptCommands) {
		return scriptCommands.stream().map(c -> appendClipNamePrefix(c, prefix)).collect(Collectors.toList());
	}
	
	private AvsScriptCommand appendClipNamePrefix(final AvsScriptCommand command, final String prefix) {
		final String commandIdentifier = command.getIdentifier();
		final StringBuilder modifiedCommand = new StringBuilder(command.getCommand());
		
		if(commandIdentifier.equals(AvsScriptCommand.COMMENT) || commandIdentifier.equals(AvsScriptCommand.GENERIC)
				|| commandIdentifier.equals(AvsScriptCommand.LOAD_PLUGIN) || commandIdentifier.equals(AvsScriptCommand.RESIZE)) {
			return command;
		}
		
		if(!(commandIdentifier.equals(AvsScriptCommand.DGDECODE_IDENTIFIER) ||
				commandIdentifier.equals(AvsScriptCommand.DGSOURCE_IDENTIFIER) ||
				commandIdentifier.equals(AvsScriptCommand.QTGMC))) {
			modifiedCommand.insert(0, '.');
			modifiedCommand.insert(0, prefix);
		}
		
		if(commandIdentifier.equals(AvsScriptCommand.QTGMC)) {
			modifiedCommand.insert(modifiedCommand.indexOf("(") + 1, prefix + ",");
		} 
		
		modifiedCommand.insert(0, '=');
		modifiedCommand.insert(0, prefix);
		
		return new AvsScriptCommand(commandIdentifier, modifiedCommand.toString());
	}
}