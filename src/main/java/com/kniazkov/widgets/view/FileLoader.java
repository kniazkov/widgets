/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.UploadEvent;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.protocol.RequestNextChunk;
import java.util.Map;
import java.util.TreeMap;

/**
 * A specialized button widget that handles file uploads from the client.
 * <p>
 * This widget manages the upload process by receiving file chunks via events,
 * assembling them into complete files, and providing progress tracking.
 * It automatically requests the next chunk after processing each received chunk.
 */
public class FileLoader extends Button implements HasMultipleInput {
    /**
     * Map of currently uploading files, keyed by their unique file ID.
     */
    private final Map<Integer, UploadingFile> uploading = new TreeMap<>();

    /**
     * Controller to notify when a new file upload starts.
     */
    private Controller<UploadingFile> onSelectCtrl = Controller.stub();

    /**
     * Constructs a new FileLoader with default text.
     */
    public FileLoader() {
        super();
    }

    /**
     * Constructs a new FileLoader with the specified button text.
     *
     * @param text the text to display on the button
     */
    public FileLoader(final String text) {
        super(text);
    }

    /**
     * Constructs a new FileLoader with the specified style and text.
     *
     * @param style the button style to apply
     * @param text the text to display on the button
     */
    public FileLoader(final ButtonStyle style, final String text) {
        super(style, text);
    }

    @Override
    public String getType() {
        return "file loader";
    }

    /**
     * Processes an incoming upload event containing a file chunk.
     * <p>
     * If this is the first chunk of a new file, creates a new UploadingFile instance.
     * If the file is already being uploaded, adds the chunk to the existing file.
     * Automatically requests the next chunk from the client after processing.
     *
     * @param event the upload event containing file metadata and chunk data
     */
    public void handleUploadEvent(final UploadEvent event) {
        UploadingFile file = this.uploading.get(event.fileId);
        if (file == null) {
            file = new UploadingFile(event);
            this.uploading.put(event.fileId, file);
            this.onSelectCtrl.handleEvent(file);
        } else {
            file.handleUploadEvent(event);
        }
        if (event.chunkIndex >= 0) {
            this.pushUpdate(new RequestNextChunk(this.getId()));
        }
    }

    /**
     * Registers a controller to be notified when a new file upload starts.
     *
     * @param ctrl the controller to notify when a file upload begins
     */
    public void onSelect(final Controller<UploadingFile> ctrl) {
        this.onSelectCtrl = ctrl;
    }

    /**
     * Returns the model that stores the accepted files pattern for this view.
     *
     * @return the accepted files model
     */
    public Model<String> getAcceptedFilesModel() {
        return this.getModel(State.ANY, Property.ACCEPTED_FILES);
    }

    /**
     * Sets a new accepted files model for this view.
     *
     * @param model the accepted files model to set
     */
    public void setAcceptedFilesModel(Model<String> model) {
        this.setModel(State.ANY, Property.ACCEPTED_FILES, model);
    }

    /**
     * Returns the current accepted files pattern from the model.
     *
     * @return the current accepted files pattern (e.g., ".pdf,.docx" or "image/*")
     */
    public String getAcceptedFiles() {
        return this.getAcceptedFilesModel().getData();
    }

    /**
     * Updates the accepted files pattern in the model.
     *
     * @param pattern the new accepted files pattern
     */
    public void setAcceptedFiles(String pattern) {
        this.getAcceptedFilesModel().setData(pattern);
    }

    /**
     * Configures this view to accept all file types.
     * <p>
     * Equivalent to setting an empty pattern or wildcard, allowing any file to be selected.
     */
    public void acceptAllFiles() {
        this.setAcceptedFiles("");
    }

    /**
     * Configures this view to accept only image files.
     * <p>
     * Sets the pattern to "image/*", allowing all image MIME types (JPEG, PNG, GIF, etc.).
     */
    public void acceptImagesOnly() {
        this.setAcceptedFiles("image/*");
    }
}
