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

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;
import org.matic.x264batcher.utils.Helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * A tab displaying the logger output and offering the user to store it to a file.
 *
 * @author Vedran Matic
 */
public final class LogTabView {

    private final ListView<LogEntry> loggerView;
    private final EncoderLogger logger;
    private final Window parent;
    private final Tab logTab;

    private final CheckBox enableFileLoggingCheck = new CheckBox("File: ");
    private final ComboBox<LogEntry.Severity> logFilterCombo = new ComboBox<>();
    private final Button browseLogFileButton = new Button("Browse...");
    private final Button clearLogButton = new Button("Clear Log");
    private final TextField logFilePathField = new TextField();
    private final TextField logFilterField = new TextField();

    private final FileLogger fileLogger = new FileLogger();

    public LogTabView(final Window parent, final ListView<LogEntry> loggerView, final EncoderLogger logger) {
        this.loggerView = loggerView;
        this.parent = parent;
        this.logger = logger;
        initComponents();
        setupActionHandlers();
        logTab = buildLogTab(loggerView);
    }

    public Tab getTab() {
        return logTab;
    }

    private void initComponents() {
        logFilterCombo.getItems().setAll(LogEntry.Severity.values());
        logFilterCombo.getSelectionModel().select(LogEntry.Severity.ALL);

        enableFileLoggingCheck.setAlignment(Pos.CENTER_LEFT);

        logFilePathField.setPromptText("<target log output file>");
        logFilterField.setPromptText("<type text to search for in the log>");

        final BooleanBinding logToFileDisabledBinding = enableFileLoggingCheck.disabledProperty()
                .or(enableFileLoggingCheck.selectedProperty().not());
        logFilePathField.disableProperty().bind(logToFileDisabledBinding);
        browseLogFileButton.disableProperty().bind(logToFileDisabledBinding);
        clearLogButton.disableProperty().bind(Bindings.size(loggerView.getItems()).isEqualTo(0));
    }

    private void setupActionHandlers() {
        loggerView.getItems().addListener((ListChangeListener.Change<?extends LogEntry> c) -> {
            if(c.next() && c.wasAdded()) {
                final List<?extends LogEntry> addedLogEntries = c.getAddedSubList();
                //addedLogEntries.forEach(logEntry -> fileLogger.write(logEntry));
            }
        });
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
        clearLogButton.setOnAction(e -> logger.clear());
        browseLogFileButton.setOnAction(e -> onBrowseForTargetLogFile());
    }

    private void onBrowseForTargetLogFile() {
        final String initialFileChooserPath = Helper.loadPreference(
                Helper.LAST_OUTPUT_PATH_PROPERTY, System.getProperty("user.home"));
        final File selectedFile = Helper.showSaveFileChooser(parent, "Select log output file",
                Files.exists(Paths.get(initialFileChooserPath))? initialFileChooserPath :
                    System.getProperty("user.home"));
        if(selectedFile != null) {
            if(selectedFile.exists()) {
                final boolean overwriteFile = Helper.showAlert(parent, Alert.AlertType.WARNING,
                        "The file already exists. Overwrite?", "File Exists");
                if(!overwriteFile) {
                    return;
                }
            }
            try {
                fileLogger.closeForWriting();
                fileLogger.openForWriting(selectedFile);
            } catch (final IOException ioe) {
                Helper.showAlert(parent, Alert.AlertType.ERROR, "Failed to open the file for writing:\n"
                        + ioe.getMessage(), "File Open Error");
                return;
            }
            logFilePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    private <T> Tab buildLogTab(final ListView<T> loggerView) {
        final ScrollPane loggerViewScroll = new ScrollPane();
        loggerViewScroll.setContent(loggerView);
        loggerViewScroll.setFitToHeight(true);
        loggerViewScroll.setFitToWidth(true);

        final BorderPane logFilePane = new BorderPane();
        logFilePane.setLeft(enableFileLoggingCheck);
        logFilePane.setCenter(logFilePathField);
        logFilePane.setRight(browseLogFileButton);

        final Insets componentInsets = new Insets(5, 5, 5, 0);

        BorderPane.setMargin(enableFileLoggingCheck, componentInsets);
        BorderPane.setMargin(logFilePathField, componentInsets);
        BorderPane.setMargin(browseLogFileButton, componentInsets);

        final BorderPane logPane = new BorderPane();
        logPane.setTop(buildLogControlsPane());
        logPane.setCenter(loggerViewScroll);
        logPane.setBottom(logFilePane);

        BorderPane.setMargin(loggerViewScroll, new Insets(0, 5, 0, 5));
        BorderPane.setMargin(logFilePane, new Insets(0, 0, 0, 5));

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
}