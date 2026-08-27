/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.common.UploadedFile;
import com.kniazkov.widgets.common.Utils;
import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.UploadEvent;
import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.model.Model;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Tracks one selected file, accepts binary chunks, and publishes upload progress.
 */
public class UploadingFile {
    /**
     * Widget that owns the upload descriptor.
     */
    private final Widget<?> widget;

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
     * Binary chunk size configured for this widget tree.
     */
    private final int chunkSize;

    /**
     * Binary chunks retained until the complete file has been assembled.
     */
    private final byte[][] content;

    /**
     * SHA-256 digest of every accepted chunk, retained for idempotent retries.
     */
    private final byte[][] digests;

    /**
     * Number of chunks that have been accepted so far.
     */
    private int uploadedChunksCount;

    /**
     * Index of the first chunk that has not been accepted yet.
     */
    private int nextMissingChunk;

    /**
     * Total number of chunks expected for this file.
     */
    private final int totalChunks;

    /**
     * The fully assembled file, available once all chunks are received.
     */
    private UploadedFile fullyUploadedFile;

    /**
     * Controller to notify when the file is completely uploaded.
     */
    private Controller<UploadedFile> onLoadCtrl = Controller.stub();

    /**
     * Model tracking the upload progress as a percentage from zero to one hundred.
     */
    private Model<Integer> percentage;

    /**
     * Constructs a zero-progress descriptor from selected-file metadata.
     *
     * @param widget widget that owns the selected file
     * @param event selected-file metadata
     */
    UploadingFile(final Widget<?> widget, final UploadEvent event) {
        final Options options = widget.getRootWidget()
            .orElseThrow(() -> new IllegalArgumentException("Widget is not attached to a root"))
            .getOptions();
        validate(event, options);
        this.widget = widget;
        this.name = event.name;
        this.type = event.type == null || event.type.isEmpty()
            ? Utils.getContentTypeByExtension(event.name)
            : event.type;
        this.size = event.size;
        this.chunkSize = options.getChunkSize();
        this.totalChunks = event.totalChunks;
        this.content = new byte[this.totalChunks][];
        this.digests = new byte[this.totalChunks][];
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
     * Returns a model that tracks the upload progress as a percentage.
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
     * Accepts a binary chunk or verifies that a repeated chunk is identical.
     *
     * @param chunkIndex zero-based chunk index
     * @param data binary chunk content
     * @return whether the chunk is valid for this upload
     */
    boolean handleUploadChunk(final int chunkIndex, final byte[] data) {
        if (data == null || chunkIndex < 0 || chunkIndex >= this.totalChunks
                || data.length != this.expectedChunkSize(chunkIndex)) {
            return false;
        }
        final byte[] digest = digest(data);
        if (this.digests[chunkIndex] != null) {
            if (!MessageDigest.isEqual(this.digests[chunkIndex], digest)) {
                return false;
            }
            return this.content[chunkIndex] == null
                || Arrays.equals(this.content[chunkIndex], data);
        }

        this.content[chunkIndex] = data.clone();
        this.digests[chunkIndex] = digest;
        this.uploadedChunksCount++;
        while (this.nextMissingChunk < this.totalChunks
                && this.digests[this.nextMissingChunk] != null) {
            this.nextMissingChunk++;
        }
        if (this.percentage != null) {
            this.percentage.setData(this.uploadedChunksCount * 100 / this.totalChunks);
        }
        if (this.uploadedChunksCount == this.totalChunks) {
            this.fullyUploadedFile = this.createFile();
            Arrays.fill(this.content, null);
            this.runOnLoadHandler();
        }
        return true;
    }

    /**
     * Returns the first chunk index that the server has not received.
     *
     * @return first missing index, or total chunk count after completion
     */
    int getNextMissingChunk() {
        return this.nextMissingChunk;
    }

    /**
     * Returns whether every binary chunk has been accepted.
     *
     * @return true after the complete file has been assembled
     */
    boolean isComplete() {
        return this.fullyUploadedFile != null;
    }

    /**
     * Assembles the complete file from all accepted binary chunks.
     *
     * @return the fully assembled file
     */
    private UploadedFile createFile() {
        final byte[] data = new byte[this.size];
        int offset = 0;
        for (final byte[] chunk : this.content) {
            System.arraycopy(chunk, 0, data, offset, chunk.length);
            offset += chunk.length;
        }
        return new UploadedFile(this.name, this.type, data);
    }

    /**
     * Launches the completion handler after the current synchronization operation.
     */
    private void runOnLoadHandler() {
        final Optional<RootWidget> root = this.widget.getRootWidget();
        if (root.isPresent()) {
            synchronized (root.get()) {
                new Thread(
                    () -> this.onLoadCtrl.handleEvent(this.fullyUploadedFile),
                    "widgets-upload-handler"
                ).start();
            }
        }
    }

    /**
     * Returns the only valid byte count for a given chunk index.
     */
    private int expectedChunkSize(final int chunkIndex) {
        if (chunkIndex < this.totalChunks - 1) {
            return this.chunkSize;
        }
        return this.size - chunkIndex * this.chunkSize;
    }

    /**
     * Validates browser-controlled upload metadata before allocating chunk storage.
     */
    private static void validate(final UploadEvent event, final Options options) {
        if (event == null || event.fileId <= 0 || event.name == null || event.name.isEmpty()
                || event.name.indexOf('/') >= 0 || event.name.indexOf('\\') >= 0
                || event.name.indexOf('\0') >= 0 || event.size < 0
                || event.size > options.getMaxFileSize()) {
            throw new IllegalArgumentException("Invalid upload metadata");
        }
        final int expectedChunks = (int) Math.max(
            1L,
            ((long) event.size + options.getChunkSize() - 1L) / options.getChunkSize()
        );
        if (event.totalChunks != expectedChunks) {
            throw new IllegalArgumentException("Invalid upload chunk count");
        }
    }

    /**
     * Calculates the digest used to verify a repeated chunk after response loss.
     */
    private static byte[] digest(final byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (final NoSuchAlgorithmException error) {
            throw new AssertionError("SHA-256 is unavailable", error);
        }
    }
}
