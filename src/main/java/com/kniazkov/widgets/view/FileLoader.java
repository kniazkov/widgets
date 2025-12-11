/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.UploadEvent;
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
public class FileLoader extends Button {
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
}
