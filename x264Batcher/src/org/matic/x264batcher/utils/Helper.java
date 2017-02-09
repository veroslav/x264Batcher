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
package org.matic.x264batcher.utils;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import org.matic.ApplicationMain;
import org.matic.x264batcher.model.EncoderPreset;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Various utility methods that are useful but don't belong to any specific class.
 * 
 * @author Vedran Matic
 *
 */
public final class Helper {
	
	public static String ENCODER_JOB_LIMIT_PROPERTY = "encoder.job.limit";
	public static String SHUTDOWN_COMPUTER_PROPERTY = "shutdown.when.done";
	public static String MKVMERGE_EXE_PATH_PROPERTY = "mkvmerge.exe.path";
	public static String X264_EXE_PATH_PROPERTY = "x264.exe.path";

	public static String SAR_NOMINATOR_PROPERTY = "dar.nominator";
	public static String SAR_DENOMINATOR_PROPERTY = "dar.denominator";
	public static String PERFORM_CLEANUP_PROPERTY = "perform.cleanup";
	public static String LAST_OUTPUT_PATH_PROPERTY = "last.output.path";
	
	private static final String DATE_FORMAT_PATTERN = "dd/MMMM/yyyy HH:mm";

	private static String ACTIVE_ENCODER_PRESET_PROPERTY = "encoder.preset.active";
	private static String ENCODER_PRESET_LIST_PROPERTY = "encoder.preset.list";
	
	private static final String ENCODER_VALUES_DELIMITER = "@";
	private static final String ENCODER_PRESET_DELIMITER = "!";
	
	private static final Preferences PREFERENCES = Preferences.userNodeForPackage(ApplicationMain.class);
	
	/**
	 * Save an application property value to the disk.
	 * 
	 * @param name Property name
	 * @param value Property value
	 */
	public static void storePreference(final String name, final String value) {
		PREFERENCES.put(name, value);
	}

	/**
	 * Load an application property value from the disk.
	 * 
	 * @param name Property name
	 * @param defaultValue Default property value if the property doesn't yet exists
	 * @return
	 */	
	public static String loadPreference(final String name, final String defaultValue) {
		return PREFERENCES.get(name, defaultValue);
	}
	
	/**
	 * Load currently active encoder preset from the disk. If none exist, use the Default preset.
	 * 
	 * @return Active encoder preset or default one if none exists
	 */
	public static EncoderPreset loadActiveEncoderPreset() {
		final String activeEncoderPresetProperty = Helper.loadPreference(ACTIVE_ENCODER_PRESET_PROPERTY, null);
		if(activeEncoderPresetProperty == null) {
			return EncoderPreset.getDefault();
		}
		final String[] presetValues = activeEncoderPresetProperty.split(ENCODER_VALUES_DELIMITER);
		return new EncoderPreset(presetValues[0], presetValues[1]);
	}
	
	/**
	 * Save the active encoder preset.
	 * 
	 * @param activePreset Preset to save
	 */
	public static void storeActiveEncoderPreset(final EncoderPreset activePreset) {
		final StringBuilder activePresetBuilder = new StringBuilder();
		activePresetBuilder.append(activePreset.getName());
		activePresetBuilder.append(ENCODER_VALUES_DELIMITER);
		activePresetBuilder.append(activePreset.getCommand());
		
		Helper.storePreference(ACTIVE_ENCODER_PRESET_PROPERTY, activePresetBuilder.toString());
	}
	
	/**
	 * Load all of the previously saved encoder presets. A default encoder preset
	 * will always be returned if there are no other presets.
	 * 
	 * @return Stored encoder presets
	 */
	public static List<EncoderPreset> loadEncoderPresets() {
		final String encoderPresetsProperty = Helper.loadPreference(ENCODER_PRESET_LIST_PROPERTY, null);
		final List<EncoderPreset> encoderPresetList = new ArrayList<>();
		
		if(encoderPresetsProperty != null) {
			final String[] encoderPresets = encoderPresetsProperty.split(ENCODER_PRESET_DELIMITER);
			for(final String preset : encoderPresets) {
				final String[] presetValues = preset.split(ENCODER_VALUES_DELIMITER);
				encoderPresetList.add(new EncoderPreset(presetValues[0], presetValues[1]));
			}
		}
		
		final EncoderPreset defaultPreset = EncoderPreset.getDefault();
		if(!encoderPresetList.contains(defaultPreset)) {
			encoderPresetList.add(0, defaultPreset);
		}
		
		return encoderPresetList;
	}
	
	/**
	 * Save all encoder presets to disk.
	 * 
	 * @param encoderPresets Presets to save
	 */
	public static void storeEncoderPresets(final List<EncoderPreset> encoderPresets) {		
		final String encoderPresetPropertyValue = encoderPresets.stream().map(preset -> {
			final StringBuilder propertyBuilder = new StringBuilder();
			propertyBuilder.append(preset.getName());
			propertyBuilder.append(ENCODER_VALUES_DELIMITER);
			propertyBuilder.append(preset.getCommand());
			return propertyBuilder.toString();
		}).collect(Collectors.joining(ENCODER_PRESET_DELIMITER));
		
		Helper.storePreference(ENCODER_PRESET_LIST_PROPERTY, encoderPresetPropertyValue);
	}
	
	/**
	 * Initialize a computer shutdown. This is done if the user
	 * chooses the option to do so after all encoding jobs are completed.
	 * 
	 * @throws IOException If the shutdown command causes any errors
	 */
	public static void shutdownComputer() throws IOException {		
		final String shutdownCmd = "shutdown /s /t 10";
		Runtime.getRuntime().exec(shutdownCmd);
		System.exit(0);
	}
	
	/**
	 * Check whether a string consists of numbers only.
	 * 
	 * @param str String to check
	 * @return True of the string consists of numbers only, false otherwise
	 */
	public static boolean isNumber(final String str) {
		return str.chars().filter(Character::isDigit).count() == str.length();
	}
	
	/**
	 * Format milliseconds to a humanly readable time representation.
	 * 
	 * @param millis Milliseconds to format
	 * @return Humanly readable time representation
	 */
	public static String formatMillisToDate(final long millis) {
		final LocalDateTime localDateTime = LocalDateTime.ofInstant(
				Instant.ofEpochMilli(millis), ZoneId.systemDefault());
		return localDateTime.format(DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN));
	}
	
	/**
	 * Format seconds to a humanly readable time representation.
	 * 
	 * @param timeSeconds Seconds to format
	 * @return Humanly readable time representation
	 */
	public static String formatSecondsToHumanTime(final long timeSeconds) {
		long seconds = timeSeconds;
		
		final long days = TimeUnit.SECONDS.toDays(seconds);
        seconds -= TimeUnit.SECONDS.toSeconds(days);
        final long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds(hours);
        final long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);
		
        final StringBuilder result = new StringBuilder();
        
        if(days > 0) {
        	result.append(days);
        	result.append("d ");
        }
        if(hours > 0) {
        	result.append(hours);
        	result.append("h ");
        }
        if(minutes > 0) {
        	result.append(minutes);
        	result.append("m ");
        }

    	result.append(seconds);
    	result.append("s");
        
        return result.toString();
	}
	
	/**
	 * Show a custom alert dialog to the user.
	 * 
	 * @param owner Parent window
	 * @param alertType Either WARNING, ERROR or INFO
	 * @param message The information message
	 * @param title Alert's window title
	 * @return True if user accepted the alert, false if s/he cancelled it
	 */
	public static boolean showAlert(final Window owner, final AlertType alertType,
			final String message, final String title) {
		final ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
		final ButtonType[] buttons = alertType == AlertType.WARNING?
				new ButtonType[]{ButtonType.OK, buttonTypeCancel} : new ButtonType[]{ButtonType.OK};
		final Alert alert = new Alert(alertType, message, buttons);	    
	    alert.initOwner(owner);
	    alert.setHeaderText(null);	    
	    alert.setResizable(true);
	    alert.setTitle(title);
	    final Optional<ButtonType> answer = alert.showAndWait();
	    return answer.isPresent() && answer.get() == ButtonType.OK;
	}

	/**
	 * Show a file chooser window when opening file(s).
	 * 
	 * @param owner Parent window
	 * @param title File chooser's window title
	 * @param initialPath Show contents of this path when opened
	 * @param extensionFilters File type filters
	 * @param multiSelect Whether to allow multiple file selection
	 * @return Selected file(s) or null if none were chosen
	 */
	public static List<File> showOpenFileChooser(final Window owner, final String title,
		final String initialPath, final List<ExtensionFilter> extensionFilters,
		final boolean multiSelect) {
		final FileChooser fileChooser = initFileChooser(title, initialPath);
		fileChooser.getExtensionFilters().addAll(extensionFilters);

		return multiSelect? fileChooser.showOpenMultipleDialog(owner) :
				Collections.singletonList(fileChooser.showOpenDialog(owner));
	}

	/**
	 * Show a file chooser window when saving a single file.
	 *
	 * @param owner Parent window
	 * @param title File chooser's window title
	 * @param initialPath Show contents of this path when opened
	 * @return Selected file or null if none was chosen
	 */
	public static File showSaveFileChooser(final Window owner, final String title,
		final String initialPath) {
		final FileChooser fileChooser = initFileChooser(title, initialPath);

		return fileChooser.showSaveDialog(owner);
	}

	/**
	 * Show a directory chooser window.
	 * 
	 * @param owner Parent window
	 * @param title Directory chooser's window title
	 * @param initialPath Show contents of this path when opened
	 * @return Selected directory or null if none was chosen
	 */
	public static File showDirectoryChooser(final Window owner, final String title,
			final String initialPath) {
		final DirectoryChooser directoryChooser = new DirectoryChooser();				
		directoryChooser.setTitle(title);
		if(initialPath != null) {
			directoryChooser.setInitialDirectory(new File(initialPath));
		}
		
		return directoryChooser.showDialog(owner);		
	}
	
	/**
	 * Handle a drag dropped event when it occurs and get the dropped files.
	 * 
	 * @param dragEvent Drag event source
	 * @return Dropped files, or empty list if none were dropped
	 */
	public static List<String> handleDragDropped(final DragEvent dragEvent) {		       
        final List<String> droppedFilePaths = new ArrayList<>();
        final Dragboard dragBoard = dragEvent.getDragboard();

        if(dragBoard.hasFiles()) {        	
        	droppedFilePaths.addAll(dragBoard.getFiles().stream().map(
        			File::getAbsolutePath).collect(Collectors.toList()));        	                        
        }
        dragEvent.setDropCompleted(true);
        dragEvent.consume();
        
        return droppedFilePaths;
	}
	
	/**
	 * Set up the kind of files for the drop target to accept on a drag over event.
	 * 
	 * @param dragEvent Drag event source
	 * @param dragTarget Target component for the drag event
	 */
	public static void handleDragOver(final DragEvent dragEvent, final Node dragTarget) {
		if (dragEvent.getGestureSource() != dragTarget &&
                dragEvent.getDragboard().hasFiles()) {  					
            dragEvent.acceptTransferModes(TransferMode.ANY);
        }
        
        dragEvent.consume();
	}

	private static FileChooser initFileChooser(final String title, final String initialPath) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		if(initialPath != null) {
			fileChooser.setInitialDirectory(new File(initialPath));
		}
		return fileChooser;
	}
}
