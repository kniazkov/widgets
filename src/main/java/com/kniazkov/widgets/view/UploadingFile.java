/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Utils;
import com.kniazkov.widgets.common.UploadedFile;
import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.UploadEvent;
import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.model.Model;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

/**
 * Manages the state and assembly of a file being uploaded in chunks.
 * <p>
 * This class tracks the progress of a multi-chunk file upload, appends validated binary chunks
 * in order, and notifies listeners when the complete file is available.
 */
public class UploadingFile {
    /**
     * Widget used to download the file.
     */
    private final Widget<?> widget;

    /** Browser-local identifier shared by every chunk of this file. */
    private final int fileId;

    /**
     * Original filename of the file being uploaded.
     */
    private final String name;

    /**
     * MIME type of the file being uploaded.
     */
    private final String type;

    /** MIME type exactly as declared by the browser; it must stay stable across chunks. */
    private final String declaredType;

    /**
     * Total declared size of the complete file in bytes.
     */
    private final int size;

    /** Bytes received so far; capacity grows with accepted data rather than declared size. */
    private ByteArrayOutputStream content;

    /**
     * Number of chunks that have been successfully received so far.
     */
    private int uploadedChunksCount;

    /**
     * Total number of chunks expected for this file.
     */
    private final int totalChunks;

    /**
     * The fully assembled file, available once all chunks are received.
     */
    private UploadedFile fullyUploadedFile = null;

    /**
     * Controller to notify when the file is completely uploaded.
     */
    private Controller<UploadedFile> onLoadCtrl = Controller.stub();

    /**
     * Model tracking the upload progress as a percentage (0-100).
     */
    private Model<Integer> percentage = null;

    /**
     * Constructs a new UploadingFile instance from the initial upload event.
     *
     * @param widget Widget used to download the file
     * @param event the first upload event containing file metadata and possibly the first chunk
     */
    UploadingFile(final Widget<?> widget, final UploadEvent event) {
        if (!event.isValid() || event.chunkIndex != 0) {
            throw new IllegalArgumentException("The first upload chunk is invalid");
        }
        this.widget = widget;
        this.fileId = event.fileId;
        this.name = event.name;
        this.declaredType = event.type;
        this.type = event.type.isEmpty() ? Utils.getContentTypeByExtension(event.name) : event.type;
        this.size = event.size;
        this.totalChunks = event.totalChunks;
        this.content = new ByteArrayOutputStream(
            Math.min(event.size, UploadEvent.MAX_CHUNK_SIZE)
        );
        this.storeChunk(event);
        if (event.totalChunks == 1) {
            this.fullyUploadedFile = this.createFile();
        }
    }

    /**
     * Returns the original filename of the file being uploaded.
     *
     * @return the filename
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the MIME type of the file being uploaded.
     *
     * @return the MIME type
     */
    public String getType() {
        return this.type;
    }

    /**
     * Returns the total declared size of the file in bytes.
     *
     * @return the file size
     */
    public int getSize() {
        return this.size;
    }

    /**
     * Registers a controller to be notified when the file is completely uploaded.
     * If the file is already fully uploaded, the controller is invoked immediately.
     *
     * @param ctrl the controller to notify upon completion
     */
    public void onLoad(final Controller<UploadedFile> ctrl) {
        this.onLoadCtrl = ctrl;
        if (this.fullyUploadedFile != null) {
            this.runOnLoadHandler();
        }
    }

    /**
     * Returns a model that tracks the upload progress as a percentage (0-100).
     * The model is created lazily upon first request.
     *
     * @return a model containing the current upload percentage
     */
    public Model<Integer> getLoadingPercentageModel() {
        if (this.percentage == null) {
            this.percentage = new IntegerModel();
            this.percentage.setData(this.uploadedChunksCount * 100 / this.totalChunks);
        }
        return this.percentage;
    }

    /**
     * Processes an incoming upload event containing a file chunk.
     * Updates progress tracking and assembles the complete file when all chunks are received.
     *
     * @param event the upload event containing a chunk of the file
     */
    boolean handleUploadEvent(final UploadEvent event) {
        if (!event.isValid() || !this.matches(event)) {
            return false;
        }
        if (event.chunkIndex < this.uploadedChunksCount) {
            return true;
        }
        if (event.chunkIndex != this.uploadedChunksCount) {
            return false;
        }

        this.storeChunk(event);
        if (this.percentage != null) {
            final int percent = this.uploadedChunksCount * 100 / this.totalChunks;
            this.percentage.setData(percent);
        }
        if (this.uploadedChunksCount == this.totalChunks) {
            this.fullyUploadedFile = this.createFile();
            this.runOnLoadHandler();
        }
        return true;
    }

    /**
     * Returns whether every expected chunk has been accepted.
     *
     * @return {@code true} when the complete file is available
     */
    boolean isComplete() {
        return this.fullyUploadedFile != null;
    }

    /** Returns whether a chunk belongs to this browser-local upload. */
    boolean hasFileId(final int value) {
        return this.fileId == value;
    }

    /** Appends the next validated chunk without allocating for unreceived bytes. */
    private void storeChunk(final UploadEvent event) {
        final byte[] chunk = event.getContent();
        this.content.write(chunk, 0, chunk.length);
        this.uploadedChunksCount++;
    }

    /** Returns whether a later chunk still describes the same selected file. */
    private boolean matches(final UploadEvent event) {
        return this.fileId == event.fileId
            && this.size == event.size
            && this.totalChunks == event.totalChunks
            && this.name.equals(event.name)
            && this.declaredType.equals(event.type);
    }

    /**
     * Wraps the already assembled binary buffer in the public uploaded-file value object.
     *
     * @return the fully assembled UploadedFile
     */
    private UploadedFile createFile() {
        final byte[] data = this.content.toByteArray();
        this.content = null;
        return new UploadedFile(this.name, this.type, data);
    }

    /**
     * Launches a handler (callback) asynchronously, passing it the uploaded file.
     * The handler will not begin its work until the current synchronization thread with the client
     * has completed.
     */
    private void runOnLoadHandler() {
        final Optional<RootWidget> root = this.widget.getRootWidget();
        if (root.isPresent()) {
            final Controller<UploadedFile> controller = this.onLoadCtrl;
            final UploadedFile file = this.fullyUploadedFile;
            new Thread(() -> {
                synchronized (root.get()) {
                    controller.handleEvent(file);
                }
            }).start();
        }
    }

}
