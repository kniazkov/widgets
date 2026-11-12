/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Uploaded content. Framework-owned disk files live only during the onLoad callback.
 * Copy bytes or copyTo an application-owned path before returning or starting async work.
 */
public class UploadedFile implements AutoCloseable {
    /**
     * Browser-provided display name, never used as a storage path.
     */
    private final String name;
    /**
     * Declared content type.
     */
    private final String type;
    /**
     * Explicitly caller-owned in-memory content, or null for temporary disk storage.
     */
    private final byte[] content;
    /**
     * Owned private temporary file, or null for caller-owned byte content.
     */
    private final Path path;
    /**
     * Declared byte count.
     */
    private final int size;
    /**
     * Streams closed automatically when the callback ends, including forgotten streams.
     */
    private final List<InputStream> streams = new ArrayList<>();
    /**
     * Whether access to a temporary file has ended.
     */
    private boolean closed;

    /**
     * Creates a caller-owned byte-backed file with the historical unlimited lifetime.
     * @param name display name
     * @param type MIME type
     * @param content binary data
     */
    public UploadedFile(final String name, final String type, final byte[] content) {
        this.name = name;
        this.type = type;
        this.content = content;
        this.path = null;
        this.size = content.length;
    }

    /**
     * Takes ownership of a private temporary file; close deletes it and closes its streams.
     * @param name display name
     * @param type MIME type
     * @param path exclusively owned temporary file
     * @param size byte count
     */
    public UploadedFile(final String name, final String type, final Path path, final int size) {
        this.name = name;
        this.type = type;
        this.path = path;
        this.size = size;
        this.content = null;
    }

    /**
     * Returns original display name.
     * @return original display name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns declared MIME type.
     * @return declared MIME type
     */
    public String getType() {
        return this.type;
    }

    /**
     * Materializes a caller-owned copy. Prefer openStream/copyTo for large files.
     * @return binary content copy
     */
    public synchronized byte[] getContent() {
        this.requireOpen();
        try {
            return this.content != null ? this.content.clone() : Files.readAllBytes(this.path);
        } catch (final IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    /**
     * Returns streaming content; caller should close it before callback return.
     * @return streaming content; caller should close it before callback return
     * @throws IOException when the temporary content cannot be opened
     */
    public synchronized InputStream openStream() throws IOException {
        this.requireOpen();
        final InputStream input = this.content != null
            ? new ByteArrayInputStream(this.content) : Files.newInputStream(this.path);
        this.streams.add(input);
        return input;
    }

    /**
     * Copies content into application-owned storage with no whole-file memory allocation.
     * @param destination application-owned destination, overwritten if it exists
     * @throws IOException if copying fails
     */
    public synchronized void copyTo(final Path destination) throws IOException {
        this.requireOpen();
        if (this.content != null) {
            Files.write(destination, this.content);
        } else {
            Files.copy(this.path, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Returns byte count, also available after temporary storage is closed.
     * @return byte count, also available after temporary storage is closed
     */
    public int getSize() {
        return this.size;
    }

    /**
     * Closes every opened stream and deletes temporary storage. Safe to retry after IO failure.
     * @throws IOException if resource cleanup fails
     */
    @Override
    public synchronized void close() throws IOException {
        this.closed = this.path != null;
        IOException failure = null;
        for (final InputStream input : this.streams) {
            try {
                input.close();
            } catch (final IOException error) {
                failure = error;
            }
        }
        this.streams.clear();
        if (this.path != null) {
            Files.deleteIfExists(this.path);
        }
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Prevents reads after callback return, cancellation or expiration.
     */
    private void requireOpen() {
        if (this.closed) {
            throw new IllegalStateException("Uploaded file lifetime has ended");
        }
    }

    @Override
    public String toString() {
        return this.type != null && this.type.startsWith("text/")
            ? new String(this.getContent(), StandardCharsets.UTF_8) : this.type;
    }
}
