/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

/** Represents one validated binary file chunk sent from the browser. */
public final class UploadEvent {
    /** Maximum number of bytes carried by one HTTP upload request. */
    public static final int MAX_CHUNK_SIZE = 64 * 1024;

    /** Metadata limits keep untrusted multipart fields small and predictable. */
    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_TYPE_LENGTH = 255;

    /**
     * Unique identifier of the file being uploaded.
     * This ID remains consistent across all chunks of the same file.
     */
    public final int fileId;

    /**
     * The original name of the file as provided by the user.
     */
    public final String name;

    /**
     * MIME type of the file (e.g., "image/png", "application/pdf").
     */
    public final String type;

    /**
     * Total size of the entire file in bytes.
     */
    public final int size;

    /**
     * Binary content of this specific chunk.
     */
    private final byte[] content;

    /**
     * Zero-based index of this chunk within the total sequence.
     * The first chunk has index 0.
     */
    public final int chunkIndex;

    /**
     * Total number of chunks that constitute the complete file.
     */
    public final int totalChunks;

    /**
     * Creates an immutable upload chunk description.
     *
     * @param fileId browser-local file identifier
     * @param name original file name
     * @param type declared MIME type, or an empty string
     * @param size declared complete file size
     * @param content binary bytes for this chunk
     * @param chunkIndex zero-based chunk index
     * @param totalChunks declared number of chunks
     */
    public UploadEvent(final int fileId, final String name, final String type, final int size,
            final byte[] content, final int chunkIndex, final int totalChunks) {
        this.fileId = fileId;
        this.name = name;
        this.type = type;
        this.size = size;
        this.content = content == null ? null : content.clone();
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
    }

    /**
     * Validates chunk geometry before any server-side buffer is allocated.
     *
     * @return {@code true} when metadata and binary length are internally consistent
     */
    public boolean isValid() {
        if (this.fileId <= 0 || !isSafeFileName(this.name) || !isSafeContentType(this.type)
                || this.size < 0 || this.content == null) {
            return false;
        }
        final int expectedChunks = this.size == 0
            ? 1
            : ((this.size - 1) / MAX_CHUNK_SIZE) + 1;
        if (this.totalChunks != expectedChunks
                || this.chunkIndex < 0 || this.chunkIndex >= this.totalChunks) {
            return false;
        }
        final int offset = this.chunkIndex * MAX_CHUNK_SIZE;
        final int expectedLength = Math.min(MAX_CHUNK_SIZE, this.size - offset);
        return this.content.length == expectedLength;
    }

    /**
     * Returns an isolated copy of this chunk's bytes.
     *
     * @return binary chunk content, or {@code null} when the event was constructed without data
     */
    public byte[] getContent() {
        return this.content == null ? null : this.content.clone();
    }

    /** File names are data, never paths; reject separators and platform-special forms. */
    private static boolean isSafeFileName(final String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_NAME_LENGTH
                || ".".equals(value) || "..".equals(value)
                || value.endsWith(".") || value.endsWith(" ")) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            final char character = value.charAt(index);
            if (character < 32 || character == 127 || character == '/'
                    || character == '\\' || character == ':') {
                return false;
            }
        }
        return true;
    }

    /** MIME metadata may be empty but must not contain controls or grow without a bound. */
    private static boolean isSafeContentType(final String value) {
        if (value == null || value.length() > MAX_TYPE_LENGTH) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            final char character = value.charAt(index);
            if (character < 32 || character == 127) {
                return false;
            }
        }
        return true;
    }
}
