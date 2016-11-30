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

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.matic.x264batcher.encoder.AvsParser;
import org.matic.x264batcher.encoder.EncoderController;
import org.matic.x264batcher.encoder.EncodingProgressListener;
import org.matic.x264batcher.encoder.log.EncoderLogger;
import org.matic.x264batcher.encoder.log.ListViewEncoderLogger;
import org.matic.x264batcher.encoder.log.LogEntry;
import org.matic.x264batcher.exception.EncoderException;
import org.matic.x264batcher.gui.model.ClipDimensionView;
import org.matic.x264batcher.gui.model.QueuedJob;
import org.matic.x264batcher.model.*;
import org.matic.x264batcher.utils.Helper;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The main application window that the user interacts with.
 * 
 * @author Vedran Matic
 *
 */
public final class ApplicationWindow implements EncodingProgressListener {
	
	private static final String GREEN_PROGRESS_BAR = "-fx-accent: rgb(181,230,29);";
	private static final String ORANGE_PROGRESS_BAR = "-fx-accent: rgb(255,228,135);";
	private static final String RED_PROGRESS_BAR = "-fx-accent: rgb(254,198,174);";
	
	private static final String BOLD_FONT_STYLE = "-fx-font-weight: bold";
	
	private final NumberFormat numberFormatter = NumberFormat.getInstance();
	
	private final TableView<QueuedJob> jobTable = new TableView<>();	
	
	private final TextField encoderInstancesField = new TextField();
	private final TextField mkvmergeExecField = new TextField();
	private final TextField x264ExecField = new TextField();
	
	private final Button mkvmergeExecButton = new Button("Browse...");
	private final Button x264ExecButton = new Button("Browse...");		
	private final Button quitButton = new Button("Quit");
	
	private final Button cancelAllJobsButton = new Button("Cancel All");
	private final Button encodeButton = new Button("Encode");	
	
	private final Button moveJobDownButton = new Button("[v]");	
	private final Button removeJobButton = new Button("[-]");	
	private final Button cancelJobButton = new Button("Cancel");
	private final Button editJobButton = new Button("Edit...");
	private final Button moveJobUpButton = new Button("[^]");
	private final Button addJobButton = new Button("[+]");
	
	private final Button deletePresetButton = new Button("Delete");
	private final Button editPresetButton = new Button("Edit...");	
	private final Button addPresetButton = new Button("Add");
		
	private final CheckBox encoderInstancesCheckBox = new CheckBox("Encoder instances: ");
	private final CheckBox shutdownCheckBox = new CheckBox("Shutdown computer when done");	
	
	private final ProgressBar currentJobProgressBar = new ProgressBar(0);
	private final Label currentJobProgressStatus = new Label();
	
	private final ProgressBar totalJobProgressBar = new ProgressBar(0);
	private final Label totalJobProgressStatus = new Label();
	
	private final ProgressBar cpuProgressBar = new ProgressBar(0);
	private final Label cpuProgressStatus = new Label();

	private final ComboBox<LogEntry.Severity> logFilterCombo = new ComboBox<>();
	private final ListView<EncoderPreset> encoderPresetsView = new ListView<>();
	private final ListView<LogEntry> loggerView = new ListView<>();		
	private final Button clearLogButton = new Button("Clear Log");
	private final TextField logFilterField = new TextField();
		
	private final EncoderLogger logger = new ListViewEncoderLogger(loggerView);
	private final EncoderController encoderController = new EncoderController(logger);
	private final TabPane tabPane = new TabPane();
	
	private final Stage stage;

	public ApplicationWindow(final Stage stage) {
		this.stage = stage;	
		numberFormatter.setMaximumFractionDigits(2);
		numberFormatter.setMinimumFractionDigits(2);
		
		moveJobDownButton.setTooltip(new Tooltip("Move selected job down the queue"));
		moveJobUpButton.setTooltip(new Tooltip("Move selected job up the queue"));
		removeJobButton.setTooltip(new Tooltip("Remove selected job from the queue"));
		addJobButton.setTooltip(new Tooltip("Add a new job to the queue"));
		
		initComponents();
	}
	
	/**
	 * @see EncodingProgressListener#onJobCompleted(QueuedJob)
	 */
	@Override
	public void onJobCompleted(final QueuedJob queuedJob) {
		currentJobProgressBar.setProgress(1);		
		currentJobProgressStatus.setText("Job completed");	
		
		synchronized(jobTable) {
			final ObservableList<QueuedJob> selectedJobs = jobTable.getSelectionModel().getSelectedItems();
			editJobButton.setDisable(selectedJobs.size() != 1);			
		}
	}
	
	/**
	 * @see EncodingProgressListener#onAllJobsCompleted()
	 */
	@Override
	public void onAllJobsCompleted() {
		if(shutdownCheckBox.isSelected()) {
			storeApplicationState();
			try {
				Helper.shutdownComputer();
			} catch(final IOException ioe) {
				Helper.showAlert(stage, AlertType.ERROR, "Failed to shutdown computer due to:\n"
					+ ioe.getMessage(), "Shutdown Error");
			}
		}
		else {
			enableGui(true);
			currentJobProgressBar.setProgress(0);
			currentJobProgressStatus.setText("");
			totalJobProgressBar.setProgress(0);
			totalJobProgressStatus.setText("");
			cpuProgressStatus.setText("");
			cpuProgressBar.setProgress(0);
			Helper.showAlert(stage, AlertType.INFORMATION, "All jobs have completed.", "Encoder Status");
		}
	}
	
	/**
	 * @see EncodingProgressListener#onProgressUpdate(QueuedJob, EncodingProgressView)
	 */
	@Override
	public void onProgressUpdate(final QueuedJob queuedJob, final EncodingProgressView progressView) {
		if(progressView != null) {
			final double jobPercentDone = progressView.getCurrentJobPercentDone();
			final long jobSecondsLeft = progressView.getFps() > 0.0?
					(long)((progressView.getCurrentJobTotalFrames() - progressView.getCurrentJobFramesDone())
					/ progressView.getFps()) : -1;
			
			final StringBuilder jobProgressText = new StringBuilder();
			jobProgressText.append(numberFormatter.format(100 * jobPercentDone))
				.append("% done [ ")
				.append(progressView.getCurrentJobFramesDone())
				.append("/")
				.append(progressView.getCurrentJobTotalFrames())
				.append(" ][ ")
				.append(numberFormatter.format(progressView.getFps()))
				.append(" fps ] ETA: ")
				.append(jobSecondsLeft == -1? "" : Helper.formatSecondsToHumanTime(jobSecondsLeft));
			
			final double totalPercentDone = progressView.getTotalPercentDone();
			
			final StringBuilder totalProgressText = new StringBuilder();
			totalProgressText.append(numberFormatter.format(100 * totalPercentDone))
				.append("% done [ ")
				.append(progressView.getTotalJobsDone())
				.append(" of ")
				.append(progressView.getTotalJobs())
				.append(" jobs completed ]");
						
			queuedJob.setTimeTaken(System.currentTimeMillis() - queuedJob.getTimeStarted());
			
			final double cpuLoad = progressView.getCpuLoad();
			
			Platform.runLater(() -> {
				currentJobProgressStatus.setText(jobProgressText.toString());
				currentJobProgressBar.setProgress(jobPercentDone);
				totalJobProgressStatus.setText(totalProgressText.toString());
				totalJobProgressBar.setProgress(totalPercentDone);
				cpuProgressStatus.setText(numberFormatter.format(cpuLoad * 100) + "%");
				cpuProgressBar.setProgress(cpuLoad);
			});
		}
	}
	
	private void initComponents() {
		setupJobTableColumns();
		setupEncoderPresetsTab();
		setupActionHandlers();			
		
		moveJobDownButton.setAlignment(Pos.CENTER_LEFT);
		moveJobUpButton.setAlignment(Pos.CENTER_LEFT);
		
		logFilterCombo.getItems().setAll(LogEntry.Severity.values());
		logFilterCombo.getSelectionModel().select(LogEntry.Severity.ALL);
		
		logFilterField.setPromptText("<type text to search for in the log>");
		
		cancelJobButton.setDisable(true);
		cancelAllJobsButton.setDisable(true);
		encodeButton.setDisable(true);
		removeJobButton.setDisable(true);
		editJobButton.setDisable(true);
		moveJobUpButton.setDisable(true);
		moveJobDownButton.setDisable(true);

		final String encoderLimit = Helper.loadPreference(Helper.ENCODER_JOB_LIMIT_PROPERTY, EncoderParameters.AUTO_JOB_LIMIT);
		final boolean explicitEncoderLimit = !EncoderParameters.AUTO_JOB_LIMIT.equals(encoderLimit);
		encoderInstancesField.setText(explicitEncoderLimit? encoderLimit : EncoderParameters.AUTO_JOB_LIMIT);
		encoderInstancesField.setPromptText("<Value>");
		encoderInstancesCheckBox.setSelected(explicitEncoderLimit);
		
		shutdownCheckBox.setSelected(Boolean.parseBoolean(
				Helper.loadPreference(Helper.SHUTDOWN_COMPUTER_PROPERTY, "false")));
		
		mkvmergeExecField.setPromptText("<Select path to the mkvmerge.exe file>");
		mkvmergeExecField.setText(Helper.loadPreference(
				Helper.MKVMERGE_EXE_PATH_PROPERTY, ""));
		
		x264ExecField.setPromptText("<Select path to the x264.exe file>");
		x264ExecField.setText(Helper.loadPreference(
				Helper.X264_EXE_PATH_PROPERTY, ""));
		
		currentJobProgressStatus.setStyle(BOLD_FONT_STYLE);
		currentJobProgressBar.setStyle(GREEN_PROGRESS_BAR);
		
		totalJobProgressStatus.setStyle(BOLD_FONT_STYLE);
		totalJobProgressBar.setStyle(GREEN_PROGRESS_BAR);
		
		cpuProgressStatus.setStyle(BOLD_FONT_STYLE);
		cpuProgressBar.setStyle(GREEN_PROGRESS_BAR);
		
		jobTable.setPlaceholder(new Label("Click [+] or drag-and-drop files to add jobs for encoding"));
		jobTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		
		final Scene scene = new Scene(buildContentPane(), 950, 500);
		
		stage.setScene(scene);
		stage.setOnCloseRequest(this::onShutdown);		
		stage.setTitle("x264Batcher (Dev)");
		stage.show();
	}
	
	private void setupEncoderPresetsTab() {
		final ObservableList<EncoderPreset> presets = encoderPresetsView.getItems();
		presets.addAll(Helper.loadEncoderPresets());
		
		final MultipleSelectionModel<EncoderPreset> selectionModel = encoderPresetsView.getSelectionModel(); 
		selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
		if(!presets.isEmpty()) {
			selectionModel.select(0);
		}		
		encoderPresetsView.setCellFactory(c -> new ListCell<EncoderPreset>() {
            private final Tooltip tooltip = new Tooltip();
            @Override
            protected void updateItem(final EncoderPreset encoderPreset, final boolean empty) {
                super.updateItem(encoderPreset, empty);
                if(encoderPreset != null) {
                    super.setText(encoderPreset.getName());

                    tooltip.setText(encoderPreset.getCommand());
                    super.setTooltip(tooltip);
                }
                else {
                    super.setTooltip(null);
                    super.setText(null);
                    super.setGraphic(null);
                }
            }
        });

        deletePresetButton.disableProperty().bind(
        		encoderPresetsView.getSelectionModel().selectedItemProperty().isNull());
        editPresetButton.disableProperty().bind(
        		encoderPresetsView.getSelectionModel().selectedItemProperty().isNull());
	}
	
	private void setupActionHandlers() {
		setupEncoderOptionsActionHandlers();
		setupJobQueueActionHandlers();
		setupEncoderPresetActionHandlers();
		setupLoggerActionHandlers();		
	}
	
	private void setupJobQueueActionHandlers() {
		addJobButton.setOnAction(e -> onAddJob(null));
		removeJobButton.setOnAction(e -> {
			final boolean allJobsRemoved = onRemoveJobs();
			removeJobButton.setDisable(allJobsRemoved);
			editJobButton.setDisable(allJobsRemoved);
			if(allJobsRemoved) {
				encodeButton.setDisable(true);
			}
		});		
		
		encoderController.addListener(this);
		editJobButton.setOnAction(e -> onEditJob());
		quitButton.setOnAction(this::onShutdown);
		jobTable.getSelectionModel().getSelectedItems().addListener(this::onTableSelectionChanged);
		jobTable.setRowFactory(tv -> {
			final TableRow<QueuedJob> jobRow = new TableRow<>();
			jobRow.setOnMouseClicked(e -> onTableRowClick(jobRow, e));
			return jobRow;
		});
		moveJobUpButton.setOnAction(e -> onMoveSelectedJob(-1));
		moveJobDownButton.setOnAction(e -> onMoveSelectedJob(1));
	}
	
	private void setupEncoderOptionsActionHandlers() {
		final List<ExtensionFilter> exeFileFilter = Collections.singletonList(new ExtensionFilter("Executables", "*.exe"));
		x264ExecButton.setOnAction(e -> {
			final List<File> selectedFile = Helper.showFileChooser(stage, "Select x264.exe", null, exeFileFilter, false);
			if(selectedFile != null && selectedFile.size() == 1 && selectedFile.get(0) != null) {
				x264ExecField.setText(selectedFile.get(0).getAbsolutePath());
			}
		});
		mkvmergeExecButton.setOnAction(e -> {
			final List<File> selectedFile = Helper.showFileChooser(stage, "Select mkvmerge.exe", null, exeFileFilter, false);
			if(selectedFile != null && selectedFile.size() == 1 && selectedFile.get(0) != null) {
				mkvmergeExecField.setText(selectedFile.get(0).getAbsolutePath());
			}
		});	
		encodeButton.setOnAction(e -> onEncode());
		
		encoderInstancesCheckBox.setOnAction(e -> {
			encoderInstancesField.setText(encoderInstancesCheckBox.isSelected()? null : "Auto");
			encoderInstancesField.setDisable(!encoderInstancesCheckBox.isSelected());
		});
		cpuProgressBar.progressProperty().addListener((obs, oldV, newV) -> {
			final double progress = newV == null ? 0 : newV.doubleValue();
			if(progress < 0.9) {
				cpuProgressBar.setStyle(GREEN_PROGRESS_BAR);
			} else if(progress < 0.95) {
				cpuProgressBar.setStyle(ORANGE_PROGRESS_BAR);
			} else {
				cpuProgressBar.setStyle(RED_PROGRESS_BAR);
			}
		});
	}
	
	private void setupLoggerActionHandlers() {
		loggerView.setCellFactory(c -> new ListCell<LogEntry>() {
            @Override
            protected void updateItem(final LogEntry logEntry, final boolean empty) {
                super.updateItem(logEntry, empty);
                if(logEntry != null) {
                    super.setText(logEntry.toString());
                    super.setStyle(logEntry.getStyle());
                }
                else {
                    super.setText(null);
                    super.setGraphic(null);
                }
            }
        });
		logFilterCombo.getSelectionModel().selectedItemProperty().addListener(
			(obs, oldV, newV) -> {
				final String filterText = logFilterField.getText(); 
	        	if(filterText == null || filterText.length() == 0) {        		            
		            logger.filter(newV);
		        }
		        else {
		            logger.filter(newV, filterText);
		        }
			});		
		logFilterField.textProperty().addListener(obs-> {
        	final String filterText = logFilterField.getText(); 
        	final LogEntry.Severity filterSeverity =
        		logFilterCombo.getSelectionModel().getSelectedItem();
        	if(filterText == null || filterText.length() == 0) {        		            
	            logger.filter(filterSeverity);
	        }
	        else {
	            logger.filter(filterSeverity, filterText);
	        }
        });        
		clearLogButton.disableProperty().bind(Bindings.size(loggerView.getItems()).isEqualTo(0));
		clearLogButton.setOnAction(e -> logger.clear());
	}
	
	private void setupEncoderPresetActionHandlers() {
		addPresetButton.setOnAction(e -> {
			final EncoderPresetWindow addPresetWindow = new EncoderPresetWindow(stage, null);
			final EncoderPreset encoderPreset = addPresetWindow.showAndWait();

			if(encoderPreset != null) {
				final ObservableList<EncoderPreset> presetList = encoderPresetsView.getItems();
				presetList.add(encoderPreset);
				Helper.storeEncoderPresets(presetList);
				encoderPresetsView.getSelectionModel().clearAndSelect(presetList.indexOf(encoderPreset));				
			}
			
			encoderPresetsView.requestFocus();
		});

		editPresetButton.setOnAction(e -> onEditPreset());

		deletePresetButton.setOnAction(e -> {
			final ObservableList<EncoderPreset> selectedPresets =
					encoderPresetsView.getSelectionModel().getSelectedItems();

			if(!selectedPresets.isEmpty()) {
				final Alert confirmDeleteAlert = new Alert(AlertType.WARNING,
                        "Are you sure you want to delete selected preset(s)?",
                        ButtonType.OK, ButtonType.CANCEL);
                confirmDeleteAlert.initOwner(stage);
                confirmDeleteAlert.setTitle("Delete preset");
                confirmDeleteAlert.setHeaderText(null);
                
                final Optional<ButtonType> answer = confirmDeleteAlert.showAndWait();
                if(answer.isPresent() && answer.get() == ButtonType.OK) {
                	encoderPresetsView.getItems().removeAll(selectedPresets);
                }

                encoderPresetsView.requestFocus();
			}
		});
	}
	
	private void enableGui(final boolean enabled) {
		encoderInstancesCheckBox.setDisable(!enabled);
		encoderInstancesField.setDisable(!enabled || !encoderInstancesCheckBox.isSelected());
		encoderInstancesCheckBox.setDisable(!enabled);
		encodeButton.setDisable(!enabled);
		
		mkvmergeExecButton.setDisable(!enabled);
		mkvmergeExecField.setDisable(!enabled);
		
		x264ExecButton.setDisable(!enabled);
		x264ExecField.setDisable(!enabled);		
		
		cancelAllJobsButton.setDisable(enabled);
		cancelJobButton.setDisable(enabled);
	}
	
	private void onEncode() {
		enableGui(false);
		currentJobProgressBar.setProgress(0);
		currentJobProgressStatus.setText("");
		
		final String encoderJobLimit = encoderInstancesField.getText();
		final EncoderParameters encoderParameters = new EncoderParameters(
				x264ExecField.getText(),
				mkvmergeExecField.getText(),
				EncoderParameters.AUTO_JOB_LIMIT.equals(encoderJobLimit)? 0 : Integer.parseInt(encoderJobLimit));
		
		cancelJobButton.setOnAction(e -> onCancelJob(false));
		cancelAllJobsButton.setOnAction(e -> onCancelJob(true));

		encoderController.encode(encoderParameters);				
	}
	
	private void onTableSelectionChanged(final ListChangeListener.Change<?extends QueuedJob> change) {
		if(change.next()) {				
			final ObservableList<?extends QueuedJob> selectedJobs = change.getList();			
			final boolean multiJobSelection = selectedJobs.size() > 1;
			
			editJobButton.setDisable(multiJobSelection);			
			removeJobButton.setDisable(false);
			cancelJobButton.setDisable(selectedJobs.stream().filter(
					j -> j.getJobStatus() == JobStatus.RUNNING).count() == 0);
			
			boolean moveJobUpAllowed = false;
			boolean moveJobDownAllowed = false;
			
			final ObservableList<QueuedJob> jobItems = jobTable.getItems();
			
			if(selectedJobs.size() == 1) {
				final QueuedJob selectedJob = selectedJobs.get(0);				
				final int selectedJobIndex = jobItems.indexOf(selectedJob);
				moveJobDownAllowed = selectedJobIndex < jobItems.size() - 1;
				moveJobUpAllowed = selectedJobIndex > 0;
			}
			
			final int jobCount = jobTable.getItems().size();
			moveJobUpButton.setDisable(!moveJobUpAllowed || multiJobSelection || jobCount == 1);
			moveJobDownButton.setDisable(!moveJobDownAllowed || multiJobSelection || jobCount == 1);				
		}
	}
	
	@SuppressWarnings("unchecked")
	private void setupJobTableColumns() {
		final TableColumn<QueuedJob, String> jobNameColumn = new TableColumn<>("Name");
		jobNameColumn.setCellValueFactory(v -> v.getValue().nameProperty());
		jobNameColumn.setPrefWidth(150);
		
		final TableColumn<QueuedJob, String> statusColumn = new TableColumn<>("Status");
		statusColumn.setCellValueFactory(v -> v.getValue().statusProperty());
		statusColumn.setPrefWidth(100);
		
		final TableColumn<QueuedJob, String> messageColumn = new TableColumn<>("Message");
		messageColumn.setCellValueFactory(v -> v.getValue().messageProperty());
		messageColumn.setPrefWidth(100);
		
		final TableColumn<QueuedJob, String> outputPathColumn = new TableColumn<>("Output Path");
		outputPathColumn.setCellValueFactory(v -> v.getValue().outputPathProperty());
		outputPathColumn.setPrefWidth(270);
		
		final TableColumn<QueuedJob, String> encoderPresetColumn = new TableColumn<>("Preset");
		encoderPresetColumn.setCellValueFactory(v -> v.getValue().encoderPresetProperty());
		encoderPresetColumn.setPrefWidth(125);
		
		final TableColumn<QueuedJob, String> outputSarColumn = new TableColumn<>("SAR");
		outputSarColumn.setCellValueFactory(v -> v.getValue().outputSarProperty());
		outputSarColumn.setPrefWidth(110);
		
		final TableColumn<QueuedJob, Number> timeStartedColumn = new TableColumn<>("Started");
		timeStartedColumn.setCellValueFactory(v -> v.getValue().timeStartedProperty());		
		timeStartedColumn.setPrefWidth(150);
		addCellFactory(timeStartedColumn, j -> 
			j.getJobStatus() != JobStatus.QUEUED? Helper.formatMillisToDate(j.getTimeStarted()) : "");
		
		final TableColumn<QueuedJob, Number> timeCompletedColumn = new TableColumn<>("Completed");
		timeCompletedColumn.setCellValueFactory(v -> v.getValue().timeCompletedProperty());
		timeCompletedColumn.setPrefWidth(150);
		addCellFactory(timeCompletedColumn, j -> 
			j.getJobStatus() == JobStatus.FINISHED? Helper.formatMillisToDate(j.getTimeCompleted()) : "");
		
		final TableColumn<QueuedJob, Number> timeTakenColumn = new TableColumn<>("Duration");
		timeTakenColumn.setCellValueFactory(v -> v.getValue().timeTakenProperty());
		timeTakenColumn.setPrefWidth(150);
		addCellFactory(timeTakenColumn, j -> 
			j.getJobStatus() != JobStatus.QUEUED? Helper.formatSecondsToHumanTime(j.getTimeTaken() / 1000) : "");
		
		final TableColumn<QueuedJob, Boolean> cleanupFilesColumn = new TableColumn<>("Cleanup");
		cleanupFilesColumn.setCellValueFactory(v -> v.getValue().deleteTemporaryFilesProperty());
		addCellFactory(cleanupFilesColumn, j -> j.getCleanupIntermediateFiles()? "Yes" : "No");
		
		jobTable.getColumns().setAll(jobNameColumn, statusColumn, messageColumn, outputPathColumn,
				encoderPresetColumn, outputSarColumn, timeStartedColumn,
				timeCompletedColumn, timeTakenColumn, cleanupFilesColumn);
	}
	
	private <T> void addCellFactory(final TableColumn<QueuedJob, T> column,
			final Function<QueuedJob, String> valueConverter) {
		final Callback<TableColumn<QueuedJob, T>, TableCell<QueuedJob, T>> columnCellFactory =
				c -> new TableCell<QueuedJob, T>() {
			@Override
			protected final void updateItem(final T value, final boolean empty) {
				super.updateItem(value, empty);
				if(empty) {
					super.setText(null);
					super.setGraphic(null);
				}
				else {
					//final QueuedJob item = this.getTableView().getItems().get(this.getTableRow().getIndex());
					super.setText(valueConverter.apply((QueuedJob)super.getTableRow().getUserData()));
				}
			}
		};
		column.setCellFactory(columnCellFactory);
	}
	
	private Parent buildContentPane() {
		tabPane.getTabs().addAll(buildEncoderTab(), buildJobsTab(), buildPresetsTab(), buildLogTab());
		
		final BorderPane mainPane = new BorderPane();
		mainPane.setCenter(tabPane);
		mainPane.setBottom(buildButtonsPane());
		
		//Add DnD support
		mainPane.setOnDragOver(e -> Helper.handleDragOver(e, mainPane));
		mainPane.setOnDragDropped(e -> {
			final List<String> droppedFilePaths = Helper.handleDragDropped(e);
			if(!droppedFilePaths.isEmpty()) {
				tabPane.getSelectionModel().select(1);				
				onAddJob(droppedFilePaths);
			}			
		});
		
		return mainPane;
	}
	
	private Tab buildPresetsTab() {
		final BorderPane presetsPane = new BorderPane();		
		presetsPane.setTop(buildPresetPane());

		final ScrollPane encoderPresetsViewScroll = new ScrollPane(encoderPresetsView);
		presetsPane.setCenter(encoderPresetsViewScroll);	
		
		BorderPane.setMargin(encoderPresetsViewScroll, new Insets(5, 5, 0, 5));	

		encoderPresetsViewScroll.setFitToHeight(true);
        encoderPresetsViewScroll.setFitToWidth(true);                

		final Tab presetsTab = new Tab("Presets", presetsPane);
		presetsTab.setClosable(false);
		
		return presetsTab;
	}
	
	private Pane buildPresetPane() {
		final VBox presetPane = new VBox(5);
		presetPane.getChildren().addAll(buildPresetManagementPane());

		return presetPane;
	}
	
	private Pane buildPresetManagementPane() {
		final HBox presetManagementPane = new HBox(5);
		presetManagementPane.getChildren().addAll(deletePresetButton, addPresetButton);

		final BorderPane managementButtonsPane = new BorderPane();
		managementButtonsPane.setLeft(editPresetButton);
		managementButtonsPane.setRight(presetManagementPane);

		managementButtonsPane.setPadding(new Insets(5, 5, 0, 5));

		return managementButtonsPane;
	}
	
	private Tab buildEncoderTab() {
		final VBox encoderPane = new VBox(10);
		encoderPane.getChildren().addAll(buildProgressPane(),
				buildExecutablesPathPane(), buildEncoderOptionsPane(), shutdownCheckBox);
		
		VBox.setMargin(shutdownCheckBox, new Insets(0, 0, 0, 20));
		
		final Tab encoderTab = new Tab("Encoder", encoderPane);
		encoderTab.setClosable(false);
		
		return encoderTab;		
	}
	
	private Tab buildJobsTab() {		
		final Tab jobsTab = new Tab("Jobs", buildJobsPane());
		jobsTab.setClosable(false);
		
		return jobsTab;
	}
	
	private Tab buildLogTab() {		
		final ScrollPane loggerViewScroll = new ScrollPane();
        loggerViewScroll.setContent(loggerView);
        loggerViewScroll.setFitToHeight(true);
        loggerViewScroll.setFitToWidth(true);
        
		final BorderPane logPane = new BorderPane();
		logPane.setTop(buildLogControlsPane());
		logPane.setCenter(loggerViewScroll);
		
		BorderPane.setMargin(loggerViewScroll, new Insets(0, 5, 0, 5));
		
		final Tab logTab = new Tab("Log", logPane);
		logTab.setClosable(false);
		
		return logTab;
	}
	
	private Pane buildLogControlsPane() {
		final HBox severityFilterPane = new HBox();
		severityFilterPane.getChildren().addAll(new Label("Log level: "), logFilterCombo);
		severityFilterPane.setAlignment(Pos.CENTER);
		
		final HBox fieldFilterPane = new HBox();
		fieldFilterPane.getChildren().addAll(new Label("Search log: "), logFilterField);
		fieldFilterPane.setAlignment(Pos.CENTER);
		HBox.setHgrow(logFilterField, Priority.ALWAYS);
		
		final Insets controlsInsets = new Insets(5);
		BorderPane.setMargin(severityFilterPane, controlsInsets);
		BorderPane.setMargin(fieldFilterPane, controlsInsets);
		BorderPane.setMargin(clearLogButton, controlsInsets);

		final BorderPane logControlsPane = new BorderPane();
		logControlsPane.setLeft(severityFilterPane);
		logControlsPane.setCenter(fieldFilterPane);
		logControlsPane.setRight(clearLogButton);

		return logControlsPane;
	}
	
	private Pane buildJobsPane() {
		final BorderPane jobsPane = new BorderPane();
		jobsPane.setTop(buildJobButtonsPane());
		jobsPane.setCenter(buildJobTablePane());
		
		return jobsPane;
	}
	
	private Pane buildJobTablePane() {
		final VBox jobMovementButtonsPane = new VBox(5);
		jobMovementButtonsPane.getChildren().addAll(moveJobUpButton, moveJobDownButton);
		jobMovementButtonsPane.setAlignment(Pos.TOP_LEFT);
		
		moveJobDownButton.setMaxWidth(Double.POSITIVE_INFINITY);
		moveJobUpButton.setMaxWidth(Double.POSITIVE_INFINITY);
		
		final ScrollPane jobTableScroll = new ScrollPane();
        jobTableScroll.setContent(jobTable);
        jobTableScroll.setFitToHeight(true);
        jobTableScroll.setFitToWidth(true);
		
		final BorderPane jobTablePane = new BorderPane();
		jobTablePane.setRight(jobMovementButtonsPane);
		jobTablePane.setCenter(jobTableScroll);
		
		final Insets childrenInsets = new Insets(5);
		BorderPane.setMargin(jobMovementButtonsPane, childrenInsets);
		BorderPane.setMargin(jobTableScroll, new Insets(0, 0, 0, 5));
		
		return jobTablePane;
	}
	
	private Pane buildJobButtonsPane() {
		final HBox jobLifeCyclePane = new HBox(5);
		jobLifeCyclePane.getChildren().addAll(addJobButton, removeJobButton);
		jobLifeCyclePane.setAlignment(Pos.CENTER_RIGHT);
		
		final HBox jobEditPane = new HBox(5);
		jobEditPane.getChildren().addAll(editJobButton, cancelJobButton);
		jobEditPane.setAlignment(Pos.CENTER_RIGHT);
		
		final BorderPane buttonsPane = new BorderPane();
		buttonsPane.setLeft(jobEditPane);
		buttonsPane.setRight(jobLifeCyclePane);
		
		final Insets buttonInsets = new Insets(5);
		BorderPane.setMargin(jobLifeCyclePane, buttonInsets);
		BorderPane.setMargin(jobEditPane, buttonInsets);
		
		return buttonsPane;
	}
	
	private Pane buildProgressPane() {				
		final Label currentJobProgressLabel = new Label("Current job progress: ");
		final Label totalJobProgressLabel = new Label("Total job progress: ");
		final Label cpuLoadLabel = new Label("CPU load: ");
		
		final StackPane currentJobProgressPane = new StackPane(currentJobProgressBar, currentJobProgressStatus);
		final StackPane totalJobProgressPane = new StackPane(totalJobProgressBar, totalJobProgressStatus);
		final StackPane cpuLoadPane = new StackPane(cpuProgressBar, cpuProgressStatus);
		
		final ColumnConstraints firstColumn = new ColumnConstraints();	    
		firstColumn.setPrefWidth(190);
		
		final GridPane progressPane = new GridPane();
		progressPane.setVgap(5);
		progressPane.setHgap(5);
		progressPane.setPadding(new Insets(5));
		progressPane.getColumnConstraints().addAll(firstColumn);
		
		progressPane.add(currentJobProgressLabel, 0, 0);
		progressPane.add(currentJobProgressPane, 1, 0);
		progressPane.add(totalJobProgressLabel, 0, 1);
		progressPane.add(totalJobProgressPane, 1, 1);
		progressPane.add(cpuLoadLabel, 0, 2);
		progressPane.add(cpuLoadPane, 1, 2);
		
		currentJobProgressBar.setMaxWidth(Double.POSITIVE_INFINITY);
		totalJobProgressBar.setMaxWidth(Double.POSITIVE_INFINITY);
		cpuProgressBar.setMaxWidth(Double.POSITIVE_INFINITY);
		
		GridPane.setHgrow(currentJobProgressPane, Priority.ALWAYS);
		GridPane.setHgrow(totalJobProgressPane, Priority.ALWAYS);
		GridPane.setHgrow(cpuLoadPane, Priority.ALWAYS);
		
		GridPane.setHalignment(currentJobProgressLabel, HPos.RIGHT);
		GridPane.setHalignment(totalJobProgressLabel, HPos.RIGHT);
		GridPane.setHalignment(cpuLoadLabel, HPos.RIGHT);
		
		return progressPane;
	}
	
	private Pane buildButtonsPane() {
		final HBox jobControlButtonsPane = new HBox(5);
		jobControlButtonsPane.getChildren().addAll(encodeButton, cancelAllJobsButton);
		jobControlButtonsPane.setAlignment(Pos.CENTER_RIGHT);
		
		final BorderPane buttonsPane = new BorderPane();
		buttonsPane.setLeft(jobControlButtonsPane);
		buttonsPane.setRight(quitButton);
		
		final Insets buttonInsets = new Insets(5);
		BorderPane.setMargin(jobControlButtonsPane, buttonInsets);
		BorderPane.setMargin(quitButton, buttonInsets);
		
		return buttonsPane;
	}
		
	private Pane buildEncoderOptionsPane() {
		encoderInstancesCheckBox.setPrefWidth(175);
		encoderInstancesField.setPrefWidth(80);
		
		final HBox encoderOptionsPane = new HBox(5);
		encoderOptionsPane.getChildren().addAll(encoderInstancesCheckBox, encoderInstancesField);		
		encoderOptionsPane.setAlignment(Pos.CENTER_LEFT);
		
		HBox.setMargin(encoderInstancesCheckBox, new Insets(0, 0, 0, 20));
				
		return encoderOptionsPane;
	}
	
	private Pane buildExecutablesPathPane() {
		final GridPane execPathPane = new GridPane();
		execPathPane.setVgap(5);
		execPathPane.setHgap(5);
		
		final ColumnConstraints firstColumn = new ColumnConstraints();	    
		firstColumn.setPrefWidth(190);
		execPathPane.getColumnConstraints().addAll(firstColumn);
				
		final Label x264Label = new Label("x264 executable: ");
		final Label mkvMergeLabel = new Label("mkvmerge executable: ");		
		
		execPathPane.add(x264Label, 0, 0);
		execPathPane.add(x264ExecField, 1, 0);
		execPathPane.add(x264ExecButton, 2, 0);
		execPathPane.add(mkvMergeLabel, 0, 1);
		execPathPane.add(mkvmergeExecField, 1, 1);
		execPathPane.add(mkvmergeExecButton, 2, 1);
		
		execPathPane.setPadding(new Insets(5));
		
		GridPane.setHgrow(x264ExecField, Priority.ALWAYS);
		GridPane.setHgrow(mkvmergeExecField, Priority.ALWAYS);		
		GridPane.setHalignment(x264Label, HPos.RIGHT);
		GridPane.setHalignment(mkvMergeLabel, HPos.RIGHT);		
		
		return execPathPane;
	}
	
	private void onShutdown(final Event event) {
		if(!Helper.showAlert(stage, AlertType.WARNING,
				"Are you sure you want to quit?", "Exit Confirmation")) {
			event.consume();
			return;
		}
		encoderController.cancelAll();
		storeApplicationState();
		Platform.exit();
	}
	
	private void onEditPreset() {
		final ObservableList<EncoderPreset> selectedPresets =
				encoderPresetsView.getSelectionModel().getSelectedItems();
		if(!selectedPresets.isEmpty()) {
			final EncoderPreset selectedPreset = selectedPresets.get(selectedPresets.size() - 1);
			final EncoderPresetWindow editPresetWindow = new EncoderPresetWindow(stage, selectedPreset);
			final EncoderPreset updatedPreset = editPresetWindow.showAndWait();
			
			if(updatedPreset != null) {
				final ObservableList<EncoderPreset> presetList = encoderPresetsView.getItems();
				final int selectedPresetIndex = presetList.indexOf(selectedPreset);
				presetList.remove(selectedPresetIndex);
				presetList.add(selectedPresetIndex, updatedPreset);
				Helper.storeEncoderPresets(presetList);
			}

			encoderPresetsView.requestFocus();
		}		
	}
	
	private void onMoveSelectedJob(final int direction) {
		final int selectedRowIndex = jobTable.getSelectionModel().getSelectedIndex();
		final ObservableList<QueuedJob> jobs = jobTable.getItems();
		final QueuedJob selectedJob = jobs.remove(selectedRowIndex);
		final int newSelectionIndex = selectedRowIndex + direction; 
		jobs.add(newSelectionIndex, selectedJob);
		jobTable.getSelectionModel().clearAndSelect(newSelectionIndex);
	}
	
	private void onTableRowClick(final TableRow<QueuedJob> tableRow, final MouseEvent mouseEvent) {
		if(mouseEvent.getClickCount() == 2 && !tableRow.isEmpty() && Desktop.isDesktopSupported()) {					
			final Path jobDirectoryPath = Paths.get(tableRow.getItem().getOutputPath()).getParent();
			if(jobDirectoryPath == null) {
				Helper.showAlert(stage, AlertType.ERROR, "Invalid input path.", "Path Error");
				return;
			}
					
			final File jobDirectoryFile = jobDirectoryPath.toFile();
			try {
				Desktop.getDesktop().open(jobDirectoryFile);
			} catch (final IOException ioe) {
				Helper.showAlert(stage, AlertType.ERROR,
						"An error occurred while opening the file:\n" + ioe.getMessage(), "File Open");
			}
		}
	}
	
	private void onAddJob(final List<String> jobPaths) {
		final EncoderJobParameters defaultJobParameters = EncoderJobParameters.getDefault();
		if(jobPaths != null && !jobPaths.isEmpty()) {
			defaultJobParameters.getJobInputPaths().addAll(jobPaths);
		}
		
		final JobSettingsWindow newJobWindow = new JobSettingsWindow(
				stage, encoderPresetsView.getItems(), defaultJobParameters);
		final EncoderJobParameters jobParameters = newJobWindow.showAndWait();
		
		if(jobParameters != null) {						
			try {
				final List<AvsInputFile> inputFiles = AvsParser.parseInputAvs(jobParameters.getJobInputPaths(), logger);
				
				//Check all clips' dimensions, ask user to select target dimension if those differ				
				final Map<ClipDimension, List<AvsInputFile>> uniqueClipDimensions = inputFiles.stream().collect(
						Collectors.groupingBy(AvsInputFile::getClipDimension));
				
				ClipDimension targetClipDimension;
				if(uniqueClipDimensions.size() == 1) {
					//All clips have same dimensions, no resizing will be needed
					targetClipDimension = inputFiles.get(0).getClipDimension();
				}
				else {
					final List<ClipDimensionView> dimensionViews = uniqueClipDimensions.entrySet().stream().map(cd -> {
						final List<String> clipNames = cd.getValue().stream().map(AvsInputFile::getName).collect(Collectors.toList());
						return new ClipDimensionView(cd.getKey(), clipNames);
					}).collect(Collectors.toList());
					
					final ResolutionSelectionWindow resolutionSelectionWindow = new ResolutionSelectionWindow(stage, dimensionViews);
					targetClipDimension = resolutionSelectionWindow.showAndWait();
					
					if(targetClipDimension == null) {
						//User closed the window and cancelled job addition
						return;
					}
				}
				
				synchronized(jobTable) {
					final QueuedJob jobView = new QueuedJob(jobParameters, inputFiles, targetClipDimension);
					jobTable.getItems().add(jobView);
					encoderController.add(jobView);
				}
			}
			catch(final IOException | EncoderException e) {
				Helper.showAlert(stage, AlertType.ERROR, "An error occurred while reading input file(s):\n"
						+ e.getMessage(), "Invalid input");
			}
		}
	}
	
	private void onEditJob() {			
		final ObservableList<QueuedJob> selectedJobs = jobTable.getSelectionModel().getSelectedItems();
		if(selectedJobs.size() == 1) {
			final QueuedJob editedJob = selectedJobs.get(0);			
			final EncoderJobParameters jobParameters = editedJob.getJobParameters();			
			final JobSettingsWindow editJobWindow = new JobSettingsWindow(
					stage, encoderPresetsView.getItems(), jobParameters);
			
			final EncoderJobParameters editedParameters = editJobWindow.showAndWait();
			if(editedParameters != null) {
				editedJob.setJobParameters(editedParameters);
			}
		}
	}
	
	private void onCancelJob(final boolean cancelAllJobs) {					
		if(!Helper.showAlert(stage, AlertType.WARNING,
				"Do you really want to cancel " + (cancelAllJobs?
						"all jobs" : "this job") + "?", "Confirm Cancellation")) {
			return;
		}
		
		if(cancelAllJobs) {
			encoderController.cancelAll();
		}
		else {
			encoderController.cancel();
		}			
	}
	
	private boolean onRemoveJobs() {	
		synchronized(jobTable) {
			final boolean userAccepted = Helper.showAlert(stage, AlertType.WARNING,
					"Are you sure you want to remove these jobs?", "Confirm Remove");
			if(userAccepted) {
				final ObservableList<QueuedJob> selectedJobs = jobTable.getSelectionModel().getSelectedItems();
				encoderController.remove(selectedJobs);
				return jobTable.getItems().removeAll(selectedJobs);
			}
			return false;
		}
	}	
	
	private void storeApplicationState() {
		Helper.storePreference(Helper.X264_EXE_PATH_PROPERTY, x264ExecField.getText());
		Helper.storePreference(Helper.MKVMERGE_EXE_PATH_PROPERTY, mkvmergeExecField.getText());
		Helper.storePreference(Helper.SHUTDOWN_COMPUTER_PROPERTY,
				String.valueOf(shutdownCheckBox.isSelected()));
		Helper.storePreference(Helper.ENCODER_JOB_LIMIT_PROPERTY,
				encoderInstancesField.getText());
	}
}