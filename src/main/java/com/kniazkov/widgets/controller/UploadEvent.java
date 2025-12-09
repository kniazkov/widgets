/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

/**
 * Represents a file chunk upload event sent from the client to the server.
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
     * Hex-encoded content of this specific chunk.
     */
    public String content;

    /**
     * Zero-based index of this chunk within the total sequence.
     * The first chunk has index 0.
     */
    public int chunkIndex;

    /**
     * Total number of chunks that constitute the complete file.
     */
    public int totalChunks;
}
