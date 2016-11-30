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
package org.matic.x264batcher.gui.model;

import org.matic.x264batcher.model.ClipDimension;

import java.util.List;

/**
 * A view bean for displaying a clip dimension in a list from
 * which the user can choose from in the clip resolution
 * selection window.
 * 
 * @author Vedran Matic
 *
 */
public final class ClipDimensionView {
	
	private final ClipDimension clipDimension;
	private final List<String> clipNames;

	public ClipDimensionView(final ClipDimension clipDimension, final List<String> clipNames) {
		this.clipDimension = clipDimension;
		this.clipNames = clipNames;
	}

	public ClipDimension getClipDimension() {
		return clipDimension;
	}

	@Override
	public String toString() {
		return clipDimension + " (" + clipNames + ")";
	}	
}