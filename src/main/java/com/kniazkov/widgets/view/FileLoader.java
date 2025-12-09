/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.UploadEvent;

import java.util.Map;
import java.util.TreeMap;

/**
 * ...
 */
public class FileLoader extends Button {
    /**
     * ...
     */
    final Map<Integer, UploadingFile> uploading = new TreeMap<>();

    /**
     * ...
     */
    public FileLoader() {
        super();
    }

    /**
     * ...
     * @param text
     */
    public FileLoader(final String text) {
        super(text);
    }

    /**
     * ...
     * @param style
     * @param text
     */
    public FileLoader(final ButtonStyle style, final String text) {
        super(style, text);
    }

    @Override
    public String getType() {
        return "file loader";
    }

    /**
     * ...
     * @param event
     */
    public void handleUploadEvent(final UploadEvent event) {
        UploadingFile file = this.uploading.get(event.fileId);
        if (file == null) {
            file = new UploadingFile(event);
            this.uploading.put(event.fileId, file);
        }
        file.content[event.chunkIndex] = event.content;
    }

    /**
     * ...
     */
    private static class UploadingFile {
        /**
         * ...
         */
        final String name;

        /**
         * ...
         */
        final String type;

        /**
         * ...
         */
        final int size;

        /**
         * ...
         */
        final String[] content;

        /**
         * ...
         * @param event
         */
        UploadingFile(final UploadEvent event) {
            this.name = event.name;
            this.type = event.type;
            this.size = event.size;
            this.content = new String[event.totalChunks];
        }
    }
}
