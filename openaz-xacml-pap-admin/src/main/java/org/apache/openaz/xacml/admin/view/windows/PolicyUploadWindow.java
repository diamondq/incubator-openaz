/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.apache.openaz.xacml.admin.view.windows;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vaadin.annotations.AutoGenerated;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class PolicyUploadWindow extends Window implements Receiver, SucceededListener {

	/*- VaadinEditorProperties={"grid":"RegularGrid,20","showGrid":true,"snapToGrid":true,"snapToObject":true,"movingGuides":false,"snappingDistance":10} */

	@AutoGenerated
	private VerticalLayout mainLayout;
	@AutoGenerated
	private CheckBox checkBoxImportAdvice;
	@AutoGenerated
	private CheckBox checkBoxImportObligations;
	@AutoGenerated
	private CheckBox checkBoxIgnoreStandard;
	@AutoGenerated
	private CheckBox checkBoxImportAttributes;
	@AutoGenerated
	private Upload upload;
	private static final long serialVersionUID = 1L;
	private final PolicyUploadWindow self = this;
	private static final Log logger	= LogFactory.getLog(PolicyUploadWindow.class);
	private Path directory = null;
	private Path newfile = null;
	private boolean succeeded = false;

	/**
	 * The constructor should first build the main layout, set the
	 * composition root and then do any custom initialization.
	 *
	 * The constructor will not be automatically regenerated by the
	 * visual editor.
	 */
	public PolicyUploadWindow(Path directory) {
		buildMainLayout();
		//
		// Create our main layout
		//
		this.setContent(mainLayout);
		//
		// Finish setting up the main layout
		//
		this.mainLayout.setSpacing(true);
		this.mainLayout.setMargin(true);
		
		this.checkBoxImportAdvice.setValue(true);
		this.checkBoxIgnoreStandard.setValue(true);
		this.checkBoxImportAttributes.setValue(true);
		this.checkBoxImportObligations.setValue(true);
		
		this.directory = directory;

		this.upload.addSucceededListener(this);
		this.upload.setReceiver(this);
		
		this.checkBoxImportAttributes.addValueChangeListener(new ValueChangeListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void valueChange(ValueChangeEvent event) {
				if (self.checkBoxImportAttributes.getValue()) {
					self.checkBoxIgnoreStandard.setEnabled(true);
				} else {
					self.checkBoxIgnoreStandard.setEnabled(false);
				}
			}
			
		});
	}
	
	@Override
	public OutputStream receiveUpload(String filename, String mimeType) {
		//
		// Validate the mime type
		//
		if (! mimeType.equalsIgnoreCase("text/xml")) {
			return null;
		}
		//
		// Create its new full path
		//
		this.newfile = Paths.get(self.directory.toString(), filename);
		//
		// Does it already exist?
		//
		if (Files.exists(this.newfile)) {
			//
			// TODO Prompt them to overwrite and/or bump the version???
			//
			return null;
		}
		//
		// Try to create the output stream
		//
		try {
			return new FileOutputStream(this.newfile.toFile());
		} catch (FileNotFoundException e) {
			logger.error("Failed to create uploaded file", e);
		}
		return null;
	}
	
	@Override
	public void uploadSucceeded(SucceededEvent event) {
		if (logger.isDebugEnabled()) {
			logger.debug("upload succeeded");
		}
		this.succeeded = true;
		this.close();
	}

	public Path	getUploadedFile() {
		if (this.succeeded) {
			return this.newfile;
		}
		return null;
	}
	
	public boolean importAttributes() {
		return this.checkBoxImportAttributes.getValue();
	}
	
	public boolean importObligations() {
		return this.checkBoxImportObligations.getValue();
	}
	
	public boolean importAdvice() {
		return this.checkBoxImportAdvice.getValue();
	}
	
	public boolean ignoreStandard() {
		return this.checkBoxIgnoreStandard.getValue();
	}

	@AutoGenerated
	private VerticalLayout buildMainLayout() {
		// common part: create layout
		mainLayout = new VerticalLayout();
		mainLayout.setImmediate(false);
		mainLayout.setWidth("-1px");
		mainLayout.setHeight("-1px");
		mainLayout.setMargin(true);
		mainLayout.setSpacing(true);
		
		// top-level component properties
		setWidth("-1px");
		setHeight("-1px");
		
		// upload
		upload = new Upload();
		upload.setCaption("Upload Xacml Policy File");
		upload.setImmediate(false);
		upload.setWidth("-1px");
		upload.setHeight("-1px");
		mainLayout.addComponent(upload);
		
		// checkBoxImportAttributes
		checkBoxImportAttributes = new CheckBox();
		checkBoxImportAttributes
				.setCaption("Import attributes into attribute dictionary.");
		checkBoxImportAttributes.setImmediate(false);
		checkBoxImportAttributes.setWidth("-1px");
		checkBoxImportAttributes.setHeight("-1px");
		mainLayout.addComponent(checkBoxImportAttributes);
		
		// checkBoxIgnoreStandard
		checkBoxIgnoreStandard = new CheckBox();
		checkBoxIgnoreStandard.setCaption("Ignore Standard Attributes");
		checkBoxIgnoreStandard.setImmediate(false);
		checkBoxIgnoreStandard.setWidth("-1px");
		checkBoxIgnoreStandard.setHeight("-1px");
		mainLayout.addComponent(checkBoxIgnoreStandard);
		mainLayout.setComponentAlignment(checkBoxIgnoreStandard, new Alignment(
				20));
		
		// checkBoxImportObligations
		checkBoxImportObligations = new CheckBox();
		checkBoxImportObligations
				.setCaption("Import obligations into obligation dictionary.");
		checkBoxImportObligations.setImmediate(false);
		checkBoxImportObligations.setWidth("-1px");
		checkBoxImportObligations.setHeight("-1px");
		mainLayout.addComponent(checkBoxImportObligations);
		
		// checkBoxImportAdvice
		checkBoxImportAdvice = new CheckBox();
		checkBoxImportAdvice
				.setCaption("Import advice into advice dictionary.");
		checkBoxImportAdvice.setImmediate(false);
		checkBoxImportAdvice.setWidth("-1px");
		checkBoxImportAdvice.setHeight("-1px");
		mainLayout.addComponent(checkBoxImportAdvice);
		
		return mainLayout;
	}

}
