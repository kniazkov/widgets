/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.UploadedFile;
import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.UploadEvent;
import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.model.Model;

/**
 * Manages the state and assembly of a file being uploaded in chunks.
 * <p>
 * This class tracks the progress of a multi-chunk file upload, assembles the chunks
 * in the correct order, decodes Base16 (hex) encoded content, and notifies listeners
 * when the complete file is available.
 */
public class UploadingFile {
    /**
     * Original filename of the file being uploaded.
     */
    private final String name;

    /**
     * MIME type of the file being uploaded.
     */
    private final String type;

    /**
     * Total declared size of the complete file in bytes.
     */
    private final int size;

    /**
     * Array storing Base16-encoded content chunks in their original order.
     */
    private final String[] content;

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
     * @param event the first upload event containing file metadata and possibly the first chunk
     */
    UploadingFile(final UploadEvent event) {
        this.name = event.name;
        this.type = event.type;
        this.size = event.size;
        this.content = new String[event.totalChunks];
        if (event.chunkIndex >= 0 && event.chunkIndex < event.totalChunks) {
            this.content[event.chunkIndex] = event.content;
            this.uploadedChunksCount = 1;
            if (event.totalChunks == 1) {
                this.fullyUploadedFile = this.createFile();
            }
        } else {
            this.uploadedChunksCount = 0;
        }
        this.totalChunks = event.totalChunks;
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
            ctrl.handleEvent(this.fullyUploadedFile);
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
    void handleUploadEvent(final UploadEvent event) {
        if (event.chunkIndex >= 0 && event.chunkIndex < this.totalChunks
                    && this.content[event.chunkIndex] == null) {
            this.content[event.chunkIndex] = event.content;
            this.uploadedChunksCount++;
            if (this.percentage != null) {
                int percent = this.uploadedChunksCount * 100 / this.totalChunks;
                if (percent == 99) {
                    percent = 100;
                }
                this.percentage.setData(percent);
            }
            if (this.uploadedChunksCount == this.totalChunks) {
                this.fullyUploadedFile = this.createFile();
                this.onLoadCtrl.handleEvent(this.fullyUploadedFile);
            }
        }
    }

    /**
     * Assembles the complete file from all received chunks and decodes the Base16 content.
     *
     * @return the fully assembled UploadedFile
     * @throws IllegalStateException if the decoded data size doesn't match the declared size
     */
    private UploadedFile createFile() {
        final byte[] data = decode(this.content);
        if (data.length != this.size) {
            throw new IllegalStateException(
                "The size of the uploaded file does not match the declared"
            );
        }
        return new UploadedFile(this.name, this.type, data);
    }

    /**
     * Decodes an array of Base16 (hex) encoded strings into a single byte array.
     *
     * @param content array of Base16-encoded strings, one per chunk
     * @return the concatenated and decoded byte array
     */
    private static byte[] decode(String[] content) {
        int size = 0;
        for (final String chunk : content) {
            if (chunk != null) {
                size += chunk.length();
            }
        }
        if (size % 2 != 0) {
            return new byte[0];
        }
        final byte[] result = new byte[size / 2];
        int offset = 0;
        for (String chunk : content) {
            int length = chunk.length();
            if (length % 2 != 0) {
                return new byte[0];
            }
            for (int index = 0; index < length; index += 2) {
                char char1 = chunk.charAt(index);
                char char2 = chunk.charAt(index + 1);
                int digit1 = Character.digit(char1, 16);
                int digit2 = Character.digit(char2, 16);
                if (digit1 == -1 || digit2 == -1) {
                    return new byte[0];
                }
                result[offset++] = (byte) ((digit1 << 4) | digit2);
            }
        }
        return result;
    }
}
