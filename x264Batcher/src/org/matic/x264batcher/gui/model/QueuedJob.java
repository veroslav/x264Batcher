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

import java.util.List;

import org.matic.x264batcher.model.AvsInputFile;
import org.matic.x264batcher.model.ClipDimension;
import org.matic.x264batcher.model.EncoderJobParameters;
import org.matic.x264batcher.model.EncoderPreset;
import org.matic.x264batcher.model.JobStatus;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A view to a queued encoding job. Instances of this class are used to populate the
 * GUI table of all jobs that the user have added for encoding.  
 * 
 * @author Vedran Matic
 *
 */
public final class QueuedJob {

	private final StringProperty encoderPreset = new SimpleStringProperty();
	private final StringProperty outputPath = new SimpleStringProperty();
	private final StringProperty outputSar = new SimpleStringProperty();

	private final StringProperty message = new SimpleStringProperty("");
	private final StringProperty status = new SimpleStringProperty();
	private final StringProperty name = new SimpleStringProperty();
	
	private final BooleanProperty deleteTemporaryFiles = new SimpleBooleanProperty();
	
	private final LongProperty timeStarted = new SimpleLongProperty();
	private final LongProperty timeCompleted = new SimpleLongProperty();
	private final LongProperty timeTaken = new SimpleLongProperty();
	
	private volatile JobStatus jobStatus = JobStatus.QUEUED;

	private EncoderJobParameters jobParameters;
	private final ClipDimension targetClipDimension;
	private final List<AvsInputFile> inputFiles;
	
	/**
	 * Create a view to an encoding job.
	 * 
	 * @param jobParameters	Encoding job parameters
	 * @param inputFiles Input AVS files that the job contains of
	 * @param targetClipDimension Clip dimension to use if there are variations in clip sizes
	 */
	public QueuedJob(final EncoderJobParameters jobParameters,
			final List<AvsInputFile> inputFiles, final ClipDimension targetClipDimension) {
		this.jobParameters = jobParameters;
		this.inputFiles = inputFiles;
		this.targetClipDimension = targetClipDimension;
		this.deleteTemporaryFiles.set(jobParameters.isDeleteTemporaryFiles());
		this.encoderPreset.set(jobParameters.getEncoderPreset().getName());
		this.outputPath.set(jobParameters.getJobOutputPath());
		this.outputSar.set(jobParameters.getOutputSar());
		this.name.set(jobParameters.getName());
	}
	
	public EncoderJobParameters getJobParameters() {
		return jobParameters;
	}
	
	public void setJobParameters(final EncoderJobParameters jobParameters) {
		this.jobParameters = jobParameters;
		this.deleteTemporaryFiles.set(jobParameters.isDeleteTemporaryFiles());
		this.encoderPreset.set(jobParameters.getEncoderPreset().getName());
		this.outputPath.set(jobParameters.getJobOutputPath());
		this.outputSar.set(jobParameters.getOutputSar());
		this.name.set(jobParameters.getName());
	}
	
	public List<AvsInputFile> getInputAvsFiles() {
		return inputFiles;
	}
	
	public ClipDimension getTargetClipDimension() {
		return targetClipDimension;
	}
	
	public JobStatus getJobStatus() {
		return jobStatus;
	}

	public void setStatus(final JobStatus jobStatus) {
		this.jobStatus = jobStatus;
		this.status.set(jobStatus.toString());
	}

	public void setEncoderPreset(final EncoderPreset encoderPreset) {
		this.encoderPreset.set(encoderPreset.getName());
	}
	
	public void setOutputPath(final String jobOutputPath) {
		this.outputPath.set(jobOutputPath);
	}
	
	public void setOutputSar(final String outputSar) {
		this.outputSar.set(outputSar);
	}
	
	public void setMessage(final String status) {
		this.message.set(status);
	}

	public void setName(final String name) {
		this.name.set(name);
	}
	
	public void setTimeTaken(final long timeTaken) {
		this.timeTaken.set(timeTaken);
	}
	
	public void setTimeCompleted(final long timeCompleted) {
		this.timeCompleted.set(timeCompleted);
	}
	
	public void setTimeStarted(final long timeStarted) {
		this.timeStarted.set(timeStarted);
	}
	
	public void setDeleteTemporaryFiles(final boolean deleteTemporaryFiles) {
		this.deleteTemporaryFiles.set(deleteTemporaryFiles);
	}
	
	public EncoderPreset getEncoderPreset() {
		return jobParameters.getEncoderPreset();
	}
	
	public String getOutputPath() {
		return outputPath.get();
	}
	
	public String getOutputSar() {
		return outputSar.get();
	}
	
	public String getStatus() {
		return status.get();
	}
	
	public String getMessage() {
		return message.get();
	}
	
	public String getName() {
		return name.get();
	}
	
	public long getTimeTaken() {
		return timeTaken.get();
	}
	
	public long getTimeCompleted() {
		return timeCompleted.get();
	}
	
	public long getTimeStarted() {
		return timeStarted.get();
	}
	
	public boolean getCleanupIntermediateFiles() {
		return deleteTemporaryFiles.get();
	}

	public StringProperty encoderPresetProperty() {
		return encoderPreset;
	}

	public StringProperty outputPathProperty() {
		return outputPath;
	}

	public StringProperty outputSarProperty() {
		return outputSar;
	}

	public StringProperty statusProperty() {
		return status;
	}
	
	public StringProperty messageProperty() {
		return message;
	}

	public StringProperty nameProperty() {
		return name;
	}

	public BooleanProperty deleteTemporaryFilesProperty() {
		return deleteTemporaryFiles;
	}
	
	public LongProperty timeTakenProperty() {
		return timeTaken;
	}
	
	public LongProperty timeStartedProperty() {
		return timeStarted;
	}
	
	public LongProperty timeCompletedProperty() {
		return timeCompleted;
	}

	@Override
	public String toString() {
		return "QueuedJob [encoderPreset=" + encoderPreset + ", outputPath=" + outputPath + ", outputSar=" + outputSar
				+ ", message=" + message + ", status=" + status + ", name=" + name + ", deleteTemporaryFiles="
				+ deleteTemporaryFiles + ", timeStarted=" + timeStarted + ", timeCompleted=" + timeCompleted
				+ ", timeTaken=" + timeTaken + ", jobStatus=" + jobStatus + ", jobParameters=" + jobParameters
				+ ", targetClipDimension=" + targetClipDimension + ", inputFiles=" + inputFiles + "]";
	}
	
}