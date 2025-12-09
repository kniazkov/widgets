/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;
/**
 * Represents a file that has been successfully uploaded.
 */
public class UploadedFile {
    /**
     * The original filename as provided during upload.
     */
    private final String name;

    /**
     * MIME type of the uploaded file (e.g., "image/jpeg", "application/pdf").
     */
    private final String type;

    /**
     * Binary content of the uploaded file.
     */
    private final byte[] content;

    /**
     * Constructs a new UploadedFile with the specified metadata and content.
     *
     * @param name the original filename
     * @param type the MIME type of the file
     * @param content the binary content of the file
     */
    public UploadedFile(final String name, final String type , final byte[] content) {
        this.name = name;
        this.type = type;
        this.content = content;
    }

    /**
     * Returns the original filename of the uploaded file.
     *
     * @return the filename
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the MIME type of the uploaded file.
     *
     * @return the MIME type
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the binary content of the uploaded file.
     *
     * @return a copy of the file content as a byte array
     */
    public byte[] getContent() {
        return content.clone();
    }
}
