/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.UploadEvent;
import com.kniazkov.widgets.common.UploadProtocol;
import com.kniazkov.widgets.model.Model;
import java.util.Map;
import java.util.TreeMap;

/**
 * A specialized button widget that handles file uploads from the client.
 * <p>
 * This widget registers selected files through the event stream, accepts binary chunks through
 * the upload endpoint, assembles complete files, and provides progress tracking.
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
     * Processes metadata for a file selected in the browser.
     * <p>
     * The descriptor is published to {@link #onSelect(Controller)} before any binary chunk is
     * accepted, so observers initially see zero percent progress.
     *
     * @param event selected file metadata
     */
    public void handleUploadEvent(final UploadEvent event) {
        if (this.uploading.containsKey(event.fileId)) {
            return;
        }
        try {
            final UploadingFile file = new UploadingFile(this, event);
            this.uploading.put(event.fileId, file);
            this.onSelectCtrl.handleEvent(file);
        } catch (final IllegalArgumentException ignored) {
            /*
             * Malformed external metadata is rejected at the widget boundary.
             */
        }
    }

    /**
     * Accepts one binary chunk and returns the acknowledgement serialized for the browser.
     *
     * @param fileId browser-local file identifier
     * @param chunkIndex zero-based chunk index
     * @param data binary chunk content
     * @return acknowledgement containing the first missing chunk index
     */
    public JsonObject handleUploadChunk(
            final int fileId,
            final int chunkIndex,
            final byte[] data) {
        final UploadingFile file = this.uploading.get(fileId);
        if (file == null || !file.handleUploadChunk(chunkIndex, data)) {
            return UploadProtocol.rejected();
        }
        final JsonObject response = new JsonObject();
        response.addBoolean("result", true);
        response.addNumber("nextChunk", file.getNextMissingChunk());
        response.addBoolean("complete", file.isComplete());
        return response;
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
