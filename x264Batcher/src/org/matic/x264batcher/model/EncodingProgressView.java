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
package org.matic.x264batcher.model;

import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;

/**
 * A bean containing the information aboout the progress of
 * an encoding so that it can be shown to the user through GUI.
 * 
 * @author Vedran Matic
 *
 */
public final class EncodingProgressView {

	private final OperatingSystemMXBean operatingSystemMXBean = 
			(OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
	private final long totalCurrentJobFrames;
	
	private long totalFrames = 0;
	private long totalFramesDone = 0;
	private long currentJobFramesDone = 0;
	
	private int totalJobs = 0;
	private int totalJobsDone = 0; 
	
	private double fps = 0;
	
	public EncodingProgressView(final long totalCurrentJobFrames) {
		this.totalCurrentJobFrames = totalCurrentJobFrames;
	}
	
	public double getCpuLoad() {
		return operatingSystemMXBean.getSystemCpuLoad();
	}
	
	public long getTotalFrames() {
		return totalFrames;
	}
	
	public void setTotalFrames(final long totalFrames) {
		this.totalFrames = totalFrames;
	}
	
	public long getCurrentJobTotalFrames() {
		return totalCurrentJobFrames;
	}
	
	public int getTotalJobs() {
		return totalJobs;
	}
	
	public int getTotalJobsDone() {
		return totalJobsDone;
	}
	
	public double getTotalPercentDone() {
		return ((double)totalFramesDone) / totalFrames;
	}
	
	public double getCurrentJobPercentDone() {
		return ((double)currentJobFramesDone) / totalCurrentJobFrames;
	}
	
	public void setCurrentJobFramesDone(final long currentJobFramesDone) {
		this.currentJobFramesDone = currentJobFramesDone;
	}
	
	public void setTotalFramesDone(final long totalFramesDone) {
		this.totalFramesDone = totalFramesDone;
	}
	
	public void setTotalJobs(final int totalJobs) {
		this.totalJobs = totalJobs;
	}
	
	public void setTotalJobsDone(final int totalJobsDone) {
		this.totalJobsDone = totalJobsDone;
	}
	
	public long getCurrentJobFramesDone() {
		return currentJobFramesDone;
	}
	
	public long getTotalFramesDone() {
		return totalFramesDone;
	}
	
	public void setFps(final double fps) {
		this.fps = fps;
	}
	
	public double getFps() {
		return fps;
	}
}
