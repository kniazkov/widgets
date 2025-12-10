/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.UploadedFile;
import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.UploadEvent;

import com.kniazkov.widgets.protocol.RequestNextChunk;
import java.util.Map;
import java.util.TreeMap;

/**
 * ...
 */
public class FileLoader extends Button {
    /**
     * ...
     */
    private final Map<Integer, UploadingFile> uploading = new TreeMap<>();

    /**
     * ...
     */
    private Controller<UploadedFile> onLoadCtrl = Controller.stub();

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
        if (event.totalChunks == 1) {
            this.onLoadCtrl.handleEvent(
                new UploadedFile(event.name, event.type, decode(event.content))
            );
        } else {
            UploadingFile file = this.uploading.get(event.fileId);
            if (file == null) {
                file = new UploadingFile(event);
                this.uploading.put(event.fileId, file);
                file.content[event.chunkIndex] = event.content;
            } else {
                file.content[event.chunkIndex] = event.content;
                boolean completed = true;
                for (int index = 0; index < file.content.length; index++) {
                    if (file.content[index] == null) {
                        completed = false;
                        break;
                    }
                }
                if (completed) {
                    this.onLoadCtrl.handleEvent(
                        new UploadedFile(file.name, file.type, decode(file.content))
                    );
                }
            }
            this.pushUpdate(new RequestNextChunk(this.getId()));
        }
    }

    /**
     * ...
     * @param ctrl
     */
    public void onLoad(final Controller<UploadedFile> ctrl) {
        this.onLoadCtrl = ctrl;
    }

    /**
     * ...
     * @param chunk
     * @return
     */
    private static byte[] decode(String chunk) {
        int size = chunk.length();
        if (size % 2 != 0) {
            return new byte[0];
        }
        final byte[] result = new byte[size / 2];
        int offset = 0;
        for (int index = 0; index < size; index += 2) {
            char char1 = chunk.charAt(index);
            char char2 = chunk.charAt(index + 1);
            int digit1 = Character.digit(char1, 16);
            int digit2 = Character.digit(char2, 16);
            if (digit1 == -1 || digit2 == -1) {
                return new byte[0];
            }
            result[offset++] = (byte) ((digit1 << 4) | digit2);
        }
        return result;
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
