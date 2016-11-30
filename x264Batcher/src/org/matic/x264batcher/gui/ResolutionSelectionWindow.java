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

import java.util.List;
import java.util.Optional;

import org.matic.x264batcher.gui.model.ClipDimensionView;
import org.matic.x264batcher.model.ClipDimension;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;

/**
 * A window that is presented to the user when it is detected that the clips
 * that are a part of an encoding job have differing dimensions. This window
 * gives the user an opportunity to select one of these dimensions to use
 * for the encoded clip.
 * 
 * @author Vedran Matic
 *
 */
final class ResolutionSelectionWindow {

	private final ListView<ClipDimensionView> clipDimensionView = new ListView<>();	
	private final Dialog<ButtonType> window = new Dialog<>();
	
	/**
	 * Create the window
	 * 
	 * @param owner Parent window
	 * @param dimensionViews Available clip dimensions to choose from
	 */
	ResolutionSelectionWindow(final Window owner, final List<ClipDimensionView> dimensionViews) {
		
		clipDimensionView.getItems().addAll(dimensionViews);
		
		window.initOwner(owner);
		window.setHeaderText(null);
		window.setTitle("Clip resolution resolver");
		
		setupComponents();
	}
	
	/**
	 * Make the window visible and await user input.
	 *
	 * @return Encoder preset selected by the user
	 */
	ClipDimension showAndWait() {
		final Optional<ButtonType> result = window.showAndWait();

		if(result.isPresent() && result.get() == ButtonType.OK) {
			return clipDimensionView.getSelectionModel().getSelectedItem().getClipDimension();
		}
		return null;
	}
	
	private void setupComponents() {
		clipDimensionView.getSelectionModel().select(0);
		
		window.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		window.setResizable(true);		
		window.getDialogPane().setContent(layoutContent());
	}
	
	private Node layoutContent() {
		final BorderPane mainPane = new BorderPane();
		mainPane.setPrefSize(800, 400);
		
		final Label infoLabel = new Label("Input clips have differing resolutions.\n"
				+ "Select the one you would like to use during encoding:");
		
		mainPane.setTop(infoLabel);
		
		final ScrollPane clipDimensionViewScroll = new ScrollPane(clipDimensionView);
		mainPane.setCenter(clipDimensionViewScroll);	
		
		BorderPane.setMargin(clipDimensionViewScroll, new Insets(5, 5, 0, 5));	

		clipDimensionViewScroll.setFitToHeight(true);
		clipDimensionViewScroll.setFitToWidth(true);
		
		return mainPane;
	}
}