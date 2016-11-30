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

import java.util.Optional;

import org.matic.x264batcher.model.EncoderPreset;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

/**
 * A window that offer the user to modify, add and remove encoder presets.
 * 
 * @author Vedran Matic
 *
 */
final class EncoderPresetWindow {
	private final TextField presetCommandField = new TextField();
	private final TextField presetNameField = new TextField();

	private final Dialog<ButtonType> window = new Dialog<>();

	EncoderPresetWindow(final Window owner, final EncoderPreset encoderPreset) {
		window.initOwner(owner);
		window.setTitle((encoderPreset != null? "Edit" : "Add") + " Encoder Preset");

		setupComponents(encoderPreset != null? encoderPreset : new EncoderPreset(null, null));
	}

	/**
	 * Make the window visible and await user input.
	 *
	 * @return Encoder preset selected by the user
	 */
	EncoderPreset showAndWait() {
		final Optional<ButtonType> result = window.showAndWait();

		if(result.isPresent() && result.get() == ButtonType.OK) {
			return new EncoderPreset(presetNameField.getText(), presetCommandField.getText());
		}
		return null;
	}

	private void setupComponents(final EncoderPreset encoderPreset) {
		presetCommandField.setPromptText("<command parameters to x264.exe>");
		presetNameField.setPromptText("<symbolic name for the preset>");

		presetCommandField.setText(encoderPreset.getCommand());
		presetNameField.setText(encoderPreset.getName());

		presetCommandField.setPrefWidth(450);
		presetNameField.setPrefWidth(450);

		window.setHeaderText(null);		
		window.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		final Button okButton = (Button)window.getDialogPane().lookupButton(ButtonType.OK);
		okButton.disableProperty().bind(presetNameField.textProperty().isEmpty().or(
			presetCommandField.textProperty().isEmpty()));
		okButton.setText("Save");

		window.setResizable(true);
		window.getDialogPane().setContent(layoutContent());
	}

	private Pane layoutContent() {
		final GridPane presetOptionsPane = new GridPane();
		presetOptionsPane.setVgap(5);

		final Label presetNameLabel = new Label("Preset name: ");
		presetOptionsPane.add(presetNameLabel, 0, 0);
		presetOptionsPane.add(presetNameField, 1, 0);

		final Label presetCommandLabel = new Label("Preset command: ");
		presetOptionsPane.add(presetCommandLabel, 0, 1);
		presetOptionsPane.add(presetCommandField, 1, 1);

		GridPane.setHalignment(presetNameLabel, HPos.RIGHT);
		GridPane.setHalignment(presetCommandLabel, HPos.RIGHT);

		GridPane.setHgrow(presetNameField, Priority.ALWAYS);
		GridPane.setHgrow(presetCommandField, Priority.ALWAYS);

		presetOptionsPane.setPadding(new Insets(5, 5, 0, 5));

		return presetOptionsPane;
	}
}
