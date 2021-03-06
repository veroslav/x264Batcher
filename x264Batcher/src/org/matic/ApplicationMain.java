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
package org.matic;

import org.matic.x264batcher.gui.ApplicationWindow;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * The main class, this is where the program execution begins.
 * 
 * @author Vedran Matic
 *
 */
public final class ApplicationMain extends Application {

	/**
	 * Main application execution entry point. Used when the application
	 * packaging is performed by other means than by JavaFX.
	 */
	public static void main(final String[] args) {
		launch(args);	
	}

	/**
	 * @see Application#start(Stage)
	 */
	@Override
	public void start(final Stage stage) throws Exception {
		new ApplicationWindow(stage);
	}
}
