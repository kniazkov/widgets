/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.UploadEvent;
import com.kniazkov.widgets.model.Model;

/**
 * A specialized button widget that handles file uploads from the client.
 * <p>
 * This widget manages the upload process by receiving file chunks via events,
 * assembling them into complete files, and providing progress tracking.
 * Browser backpressure ensures that the next binary chunk is sent only after the server accepts
 * the current one.
 */
public class FileLoader extends Button implements HasMultipleInput {
    /** At most one file is active because the browser applies per-widget backpressure. */
    private UploadingFile uploading;

    /** Minimal metadata retained so a lost final acknowledgement can be retried safely. */
    private CompletedUpload completed;

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
     * Invalid chunks and metadata changes are rejected without mutating upload state.
     *
     * @param event the upload event containing file metadata and chunk data
     * @return {@code true} when the chunk was accepted
     */
    public boolean handleUploadEvent(final UploadEvent event) {
        if (event == null || !event.isValid()) {
            return false;
        }
        if (this.uploading == null) {
            if (this.completed != null && this.completed.matches(event)) {
                return true;
            }
            if (event.chunkIndex != 0) {
                return false;
            }
            this.startUpload(event);
        } else if (!this.uploading.hasFileId(event.fileId)) {
            if (event.chunkIndex != 0) {
                return false;
            }
            // A new first chunk means the browser abandoned the previous rejected upload.
            this.startUpload(event);
        } else {
            if (!this.uploading.handleUploadEvent(event)) {
                return false;
            }
        }
        if (this.uploading.isComplete()) {
            this.completed = new CompletedUpload(event);
            this.uploading = null;
        }
        return true;
    }

    /** Starts one sequential upload after all boundary validation has succeeded. */
    private void startUpload(final UploadEvent event) {
        this.uploading = new UploadingFile(this, event);
        this.onSelectCtrl.handleEvent(this.uploading);
    }

    /** Metadata-only record used to acknowledge a repeated final chunk without reloading a file. */
    private static final class CompletedUpload {
        private final int fileId;
        private final String name;
        private final String type;
        private final int size;
        private final int chunkIndex;
        private final int totalChunks;

        CompletedUpload(final UploadEvent event) {
            this.fileId = event.fileId;
            this.name = event.name;
            this.type = event.type;
            this.size = event.size;
            this.chunkIndex = event.chunkIndex;
            this.totalChunks = event.totalChunks;
        }

        boolean matches(final UploadEvent event) {
            return event != null && event.isValid()
                && event.fileId == this.fileId
                && event.name.equals(this.name)
                && event.type.equals(this.type)
                && event.size == this.size
                && event.chunkIndex == this.chunkIndex
                && event.totalChunks == this.totalChunks;
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
