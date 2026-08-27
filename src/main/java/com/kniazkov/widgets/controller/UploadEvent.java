/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

/**
 * Represents metadata for a file selected by the browser.
 */
public class UploadEvent {
    /**
     * Unique identifier of the file being uploaded.
     * This ID remains consistent across all chunks of the same file.
     */
    public int fileId;

    /**
     * The original name of the file as provided by the user.
     */
    public String name;

    /**
     * MIME type of the file (e.g., "image/png", "application/pdf").
     */
    public String type;

    /**
     * Total size of the entire file in bytes.
     */
    public int size;

    /**
     * Total number of chunks that constitute the complete file.
     */
    public int totalChunks;
}
