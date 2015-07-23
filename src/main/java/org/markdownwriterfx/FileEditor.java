/*
 * Copyright (c) 2015 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.markdownwriterfx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import org.markdownwriterfx.editor.MarkdownEditorPane;
import org.markdownwriterfx.preview.MarkdownPreviewPane;

/**
 * Editor for a single file.
 *
 * @author Karl Tauber
 */
class FileEditor
{
	private final Tab tab = new Tab();
	private MarkdownEditorPane markdownEditorPane;
	private MarkdownPreviewPane markdownPreviewPane;

	FileEditor(Path path) {
		this.path.set(path);
		this.path.addListener((observable, oldPath, newPath) -> updateTab());

		// avoid that this is GCed
		tab.setUserData(this);

		updateTab();

		tab.setOnSelectionChanged(e -> {
			if(tab.isSelected())
				activated();
		});
	}

	Tab getTab() {
		return tab;
	}

	// path property
	private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
	Path getPath() { return path.get(); }
	void setPath(Path path) { this.path.set(path); }
	ObjectProperty<Path> pathProperty() { return path; }

	private void updateTab() {
		Path path = this.path.get();
		tab.setText((path != null) ? path.getFileName().toString() : "Untitled");
		tab.setTooltip((path != null) ? new Tooltip(path.toString()) : null);
	}

	private void activated() {
		if(tab.getContent() != null)
			return;

		// load file and create UI when the tab becomes visible the first time

		markdownEditorPane = new MarkdownEditorPane();
		markdownPreviewPane = new MarkdownPreviewPane();

		load();

		// bind preview to editor
		markdownPreviewPane.markdownASTProperty().bind(markdownEditorPane.markdownASTProperty());

		SplitPane splitPane = new SplitPane(markdownEditorPane.getNode(), markdownPreviewPane.getNode());
		tab.setContent(splitPane);
	}

	void load() {
		Path path = this.path.get();
		if (path == null)
			return;

		try {
			String markdown = new String(Files.readAllBytes(path));
			markdownEditorPane.setMarkdown(markdown);
		} catch (IOException ex) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Load");
			alert.setHeaderText(null);
			alert.setContentText(String.format(
				"Failed to load '%s'.\n\nReason: %s", path, ex.getMessage()));
			alert.showAndWait();
		}
	}

	boolean save() {
		String markdown = markdownEditorPane.getMarkdown();
		try {
			Files.write(path.get(), markdown.getBytes());
			return true;
		} catch (IOException ex) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Save");
			alert.setHeaderText(null);
			alert.setContentText(String.format(
				"Failed to save '%s'.\n\nReason: %s", path.get(), ex.getMessage()));
			alert.showAndWait();
			return false;
		}
	}
}
