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
package org.matic.x264batcher.gui;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import org.matic.x264batcher.model.EncoderJobParameters;
import org.matic.x264batcher.model.EncoderPreset;
import org.matic.x264batcher.utils.Helper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A window that shows properties of an encoder job. It is shown both
 * when adding a new job, and also when editing an existing job.
 * 
 * @author Vedran Matic
 *
 */
final class JobSettingsWindow {
	
	private final Button outputPathButton = new Button("Browse...");
	
	private final TextField sarNominatorField = new TextField();
	private final TextField sarDenominatorField = new TextField();
	private final TextField outputPathField = new TextField();
	private final TextField jobNameField = new TextField();
	
	private final Button addInputFileButton = new Button("[+]");
	private final Button removeInputFileButton = new Button("[-]");
	private final Button moveInputFileUpButton = new Button("[^]");
	private final Button moveInputFileDownButton = new Button("[v]");
	
	private final CheckBox deleteTemporaryFilesCheckBox = new CheckBox("Delete temporary files");
	private final ListView<String> inputFileList = new ListView<>();
	
	private final ComboBox<EncoderPreset> encoderPresetCombo;
	
	private final Dialog<ButtonType> window = new Dialog<>();

	/**
	 * Create a new instance of job settings window.
	 * 
	 * @param owner Parent and owner of this window 
	 * @param encoderPresets A list of all available encoder presets to set
	 * @param jobParameters The parameters for an existing (edited) job or null for a new job
	 */
	JobSettingsWindow(final Window owner, final List<EncoderPreset> encoderPresets,
					  final EncoderJobParameters jobParameters) {
		window.initOwner(owner);
		
		encoderPresetCombo = new ComboBox<>(
				FXCollections.observableArrayList(encoderPresets));
		
		initComponents();
		setUpActionHandlers();
		
		populateGui(jobParameters);		
	}
	
	/**
	 * Make this window appear on the screen. Return user changes to the job properties.
	 * 
	 * @return Edited or newly created job parameters set by the user
	 */
	EncoderJobParameters showAndWait() {
		final Optional<ButtonType> result = window.showAndWait();
		if(result.isPresent() && result.get() == ButtonType.OK) {
			storeJobState();
			final String outputSar = sarNominatorField.getText() + ":" + sarDenominatorField.getText();
			return new EncoderJobParameters(jobNameField.getText(), outputPathField.getText(), outputSar,
					new ArrayList<>(inputFileList.getItems()),
					encoderPresetCombo.getSelectionModel().getSelectedItem(),
					deleteTemporaryFilesCheckBox.isSelected());
		}
		return null;
	}
	
	private void populateGui(final EncoderJobParameters jobParameters) {
		inputFileList.getItems().addAll(jobParameters.getJobInputPaths());
		encoderPresetCombo.getSelectionModel().select(jobParameters.getEncoderPreset());
		outputPathField.setText(jobParameters.getJobOutputPath());
		jobNameField.setText(jobParameters.getName());
		
		final String[] sarTokens = jobParameters.getOutputSar().split(":");
		sarNominatorField.setText(sarTokens[0]);
		sarDenominatorField.setText(sarTokens[1]);
		deleteTemporaryFilesCheckBox.setSelected(jobParameters.isDeleteTemporaryFiles());
	}
	
	private void onTableSelectionChanged(final ListChangeListener.Change<?extends String> change) {
		if(change.next()) {				
			final ObservableList<?extends String> selectedFiles = change.getList();			
			final boolean multiJobSelection = selectedFiles.size() > 1;
			
			boolean moveJobUpAllowed = false;
			boolean moveJobDownAllowed = false;
			
			final ObservableList<String> inputFileItems = inputFileList.getItems();
			
			if(selectedFiles.size() == 1) {
				final String selectedFile = selectedFiles.get(0);				
				final int selectedFileIndex = inputFileItems.indexOf(selectedFile);
				moveJobDownAllowed = selectedFileIndex < inputFileItems.size() - 1;
				moveJobUpAllowed = selectedFileIndex > 0;
			}
			
			final int totalFiles = inputFileList.getItems().size();
			removeInputFileButton.setDisable(false);
			moveInputFileUpButton.setDisable(!moveJobUpAllowed || multiJobSelection || totalFiles == 1);
			moveInputFileDownButton.setDisable(!moveJobDownAllowed || multiJobSelection || totalFiles == 1);				
		}
	}
	
	private void storeJobState() {		
		Helper.storeActiveEncoderPreset(encoderPresetCombo.getSelectionModel().getSelectedItem());	
		Helper.storePreference(Helper.SAR_DENOMINATOR_PROPERTY, sarDenominatorField.getText());
		Helper.storePreference(Helper.SAR_NOMINATOR_PROPERTY, sarNominatorField.getText());
		Helper.storePreference(Helper.PERFORM_CLEANUP_PROPERTY,
				String.valueOf(deleteTemporaryFilesCheckBox.isSelected()));
	}
	
	private void setUpActionHandlers() {				
		outputPathButton.setOnAction(e -> {
			final String initialFileChooserPath = Helper.loadPreference(
					Helper.LAST_OUTPUT_PATH_PROPERTY, System.getProperty("user.home"));
			final File selectedOutputDir = Helper.showDirectoryChooser(window.getOwner(), "Select output directory",
					Files.exists(Paths.get(initialFileChooserPath))? initialFileChooserPath :
						System.getProperty("user.home"));
			if(selectedOutputDir != null) {
				Helper.storePreference(Helper.LAST_OUTPUT_PATH_PROPERTY, selectedOutputDir.getAbsolutePath());
				outputPathField.setText(selectedOutputDir.getAbsolutePath());
			}
		});
		addInputFileButton.setOnAction(e -> {
			final ExtensionFilter allFilesFilter = new ExtensionFilter("All files", "*.*");
			final ExtensionFilter avsFileFilter = new ExtensionFilter("AVS scripts", "*.avs");
			
			final String initialFileChooserPath = Helper.loadPreference(
					Helper.LAST_OUTPUT_PATH_PROPERTY, System.getProperty("user.home"));
			final List<File> selectedOutputDir = Helper.showOpenFileChooser(window.getOwner(), "Select AVS script(s)",
					Files.exists(Paths.get(initialFileChooserPath))? initialFileChooserPath :
						System.getProperty("user.home"), Arrays.asList(avsFileFilter, allFilesFilter), true);
			if(selectedOutputDir != null && !selectedOutputDir.isEmpty()) {				
				final List<String> addedFilePaths = selectedOutputDir.stream().map(
						File::getAbsolutePath).collect(Collectors.toList());
				final MultipleSelectionModel<String> selectionModel = inputFileList.getSelectionModel(); 				
				inputFileList.getItems().addAll(addedFilePaths);
				selectionModel.clearSelection();
				inputFileList.getItems().forEach(selectionModel::select);
			}
		});
		removeInputFileButton.setOnAction(e -> {
			final List<String> selectedInputFiles = inputFileList.getSelectionModel().getSelectedItems();
			inputFileList.getItems().removeAll(selectedInputFiles);
			removeInputFileButton.setDisable(inputFileList.getSelectionModel().isEmpty());
		});
		moveInputFileUpButton.setOnAction(e -> {
			final List<String> selectedInputFiles = inputFileList.getSelectionModel().getSelectedItems();
			if(selectedInputFiles.size() == 1) {
				final String selectedPath = selectedInputFiles.get(0); 
				final int selectedIndex = inputFileList.getItems().indexOf(selectedPath);
				if(selectedIndex != -1 && selectedIndex > 0) {
					inputFileList.getItems().remove(selectedIndex);
					inputFileList.getItems().add(selectedIndex - 1, selectedPath);
					inputFileList.getSelectionModel().clearAndSelect(selectedIndex - 1);
				}
			}
		});
		moveInputFileDownButton.setOnAction(e -> {
			final List<String> selectedInputFiles = inputFileList.getSelectionModel().getSelectedItems();
			if(selectedInputFiles.size() == 1) {
				final String selectedPath = selectedInputFiles.get(0); 
				final int selectedIndex = inputFileList.getItems().indexOf(selectedPath);
				if(selectedIndex != -1 && selectedIndex < inputFileList.getItems().size() - 1) {
					inputFileList.getItems().remove(selectedIndex);
					inputFileList.getItems().add(selectedIndex + 1, selectedPath);
					inputFileList.getSelectionModel().clearAndSelect(selectedIndex + 1);
				}
			}
		});
		inputFileList.getSelectionModel().getSelectedItems().addListener(this::onTableSelectionChanged);
		inputFileList.setOnDragOver(e -> Helper.handleDragOver(e, inputFileList));
		inputFileList.setOnDragDropped(e -> {
			final List<String> droppedFiles = Helper.handleDragDropped(e);
			if(!droppedFiles.isEmpty()) {
				inputFileList.getItems().addAll(droppedFiles);
			}
		});
	}
	
	private void initComponents() {		
		encoderPresetCombo.setMaxWidth(Double.POSITIVE_INFINITY);
		
		moveInputFileDownButton.setMaxWidth(Double.POSITIVE_INFINITY);
		moveInputFileUpButton.setMaxWidth(Double.POSITIVE_INFINITY);
		
		moveInputFileDownButton.setAlignment(Pos.CENTER_LEFT);
		moveInputFileUpButton.setAlignment(Pos.CENTER_LEFT);
		
		addInputFileButton.setTooltip(new Tooltip("Browse for and add AVS files to this job"));
		removeInputFileButton.setTooltip(new Tooltip("Remove selected file from this job"));
		moveInputFileUpButton.setTooltip(new Tooltip("Move the selected file up the queue"));
		moveInputFileDownButton.setTooltip(new Tooltip("Move the selected file down the queue"));
		
		outputPathField.setPromptText("<Select output path for encoded files>");
		outputPathField.setPrefWidth(450);
		
		inputFileList.setPlaceholder(new Label("Click [+] to browse for, or drag-and-drop, files to add to the job"));
		inputFileList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		inputFileList.setPrefHeight(200);
		
		removeInputFileButton.setDisable(true);
		moveInputFileDownButton.setDisable(true);
		moveInputFileUpButton.setDisable(true);
		
		sarDenominatorField.setMaxWidth(80);
		sarNominatorField.setMaxWidth(80);
		
		window.setHeaderText(null);
		window.setTitle("Job Settings");
		
		window.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		
		final SimpleListProperty<String> inputFileListProperty = new SimpleListProperty<>(inputFileList.getItems());
		
		final BooleanBinding okButtonBinding = outputPathField.textProperty().isEmpty().or(
				sarNominatorField.textProperty().isEmpty()).or(
						sarDenominatorField.textProperty().isEmpty()).or(inputFileListProperty.emptyProperty());
		final Button okButton = (Button)window.getDialogPane().lookupButton(ButtonType.OK);
		okButton.disableProperty().bind(okButtonBinding);

		window.setResizable(true);		
		window.getDialogPane().setContent(layoutContent());
	}
	
	private Node layoutContent() {
		final GridPane mainPane = new GridPane();
		mainPane.setVgap(5);
		mainPane.setHgap(5);
		
		final ColumnConstraints firstColumn = new ColumnConstraints();	    
		firstColumn.setPrefWidth(180);
		mainPane.getColumnConstraints().addAll(firstColumn);
		
		final Label encoderPresetLabel = new Label("Encoder preset: ");
		final Label jobNameLabel = new Label("Job name (optional): ");
		final Label outputPathLabel = new Label("Output path: ");			
		final Label outputSarLabel = new Label("Output SAR: ");
		
		final HBox outputSarPane = new HBox();
		outputSarPane.getChildren().addAll(sarNominatorField, new Label(" / "), sarDenominatorField);
		
		mainPane.add(outputPathLabel, 0, 0);
		mainPane.add(outputPathField, 1, 0);
		mainPane.add(outputPathButton, 2, 0);
		
		mainPane.add(jobNameLabel, 0, 1);
		mainPane.add(jobNameField, 1, 1);
		
		mainPane.add(encoderPresetLabel, 0, 2);
		mainPane.add(encoderPresetCombo, 1, 2);
		
		mainPane.add(outputSarLabel, 0, 3);
		mainPane.add(outputSarPane, 1, 3);
		
		mainPane.add(deleteTemporaryFilesCheckBox, 0, 4, 2, 1);
		
		final Node jobListView = buildJobListView();		
        mainPane.add(jobListView, 0, 5, 3, 1);        
        
		mainPane.setPadding(new Insets(5));
		
		GridPane.setMargin(deleteTemporaryFilesCheckBox, new Insets(0, 0, 0, 5));
		GridPane.setHgrow(encoderPresetCombo, Priority.ALWAYS);	
		GridPane.setHgrow(outputPathField, Priority.ALWAYS);
		GridPane.setHgrow(jobListView, Priority.ALWAYS);
		GridPane.setHalignment(outputSarLabel, HPos.RIGHT);
		GridPane.setHalignment(jobNameLabel, HPos.RIGHT);
		GridPane.setHalignment(encoderPresetLabel, HPos.RIGHT);
		GridPane.setHalignment(outputPathLabel, HPos.RIGHT);
		
		return mainPane;
	}
	
	private Node buildJobListView() {
		final HBox jobHandlingPane = new HBox(5);
		jobHandlingPane.getChildren().addAll(addInputFileButton, removeInputFileButton);		
		jobHandlingPane.setAlignment(Pos.CENTER_LEFT);
		
		final VBox jobMovementPane = new VBox(5);
		jobMovementPane.getChildren().addAll(moveInputFileUpButton, moveInputFileDownButton);
		
		final ScrollPane inputFileListScroll = new ScrollPane(inputFileList);
		inputFileListScroll.setFitToHeight(true);
        inputFileListScroll.setFitToWidth(true);
        
        final BorderPane contentPane = new BorderPane();
        contentPane.setTop(jobHandlingPane);
        contentPane.setRight(jobMovementPane);
        contentPane.setCenter(inputFileListScroll);
        
        final Insets childrenInsets = new Insets(5);
		BorderPane.setMargin(jobMovementPane, childrenInsets);
		BorderPane.setMargin(jobHandlingPane, childrenInsets);
		BorderPane.setMargin(inputFileListScroll, new Insets(0, 0, 0, 5));
        
        return contentPane;
	}
}