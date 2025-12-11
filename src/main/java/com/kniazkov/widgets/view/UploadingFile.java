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
 * ...
 */
public class UploadingFile {
    /**
     * ...
     */
    private final String name;

    /**
     * ...
     */
    private final String type;

    /**
     * ...
     */
    private final int size;

    /**
     * ...
     */
    private final String[] content;

    /**
     * ...
     */
    private int uploadedChunksCount;

    /**
     * ...
     */
    private final int totalChunks;

    /**
     *
     */
    private UploadedFile fullyUploadedFile = null;

    /**
     * ...
     */
    private Controller<UploadedFile> onLoadCtrl = Controller.stub();

    /**
     * ...
     */
    private Model<Integer> percentage = null;

    /**
     * ...
     * @param event
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
     * ...
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * ...
     * @return
     */
    public String getType() {
        return this.type;
    }

    /**
     * ...
     * @return
     */
    public int getSize() {
        return this.size;
    }

    /**
     * ...
     * @param ctrl
     */
    public void onLoad(final Controller<UploadedFile> ctrl) {
        this.onLoadCtrl = ctrl;
        if (this.fullyUploadedFile != null) {
            ctrl.handleEvent(this.fullyUploadedFile);
        }
    }

    /**
     * ...
     * @return
     */
    public Model<Integer> getLoadingPercentageModel() {
        if (this.percentage == null) {
            this.percentage = new IntegerModel();
            this.percentage.setData(this.uploadedChunksCount * 100 / this.totalChunks);
        }
        return this.percentage;
    }

    /**
     * ...
     * @param event
     */
    void handleUploadEvent(final UploadEvent event) {
        if (event.chunkIndex >= 0 && event.chunkIndex < this.totalChunks
                    && this.content[event.chunkIndex] == null) {
            this.content[event.chunkIndex] = event.content;
            this.uploadedChunksCount++;
            if (this.percentage != null) {
                this.percentage.setData(this.uploadedChunksCount * 100 / this.totalChunks);
            }
            if (this.uploadedChunksCount == this.totalChunks) {
                this.fullyUploadedFile = this.createFile();
                this.onLoadCtrl.handleEvent(this.fullyUploadedFile);
            }
        }
    }

    /**
     * ...
     * @return
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
     * ...
     * @param content
     * @return
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
