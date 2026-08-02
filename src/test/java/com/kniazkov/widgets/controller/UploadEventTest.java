/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Tests validation at the untrusted binary-upload boundary. */
public class UploadEventTest {
    /** An exact chunk boundary is one chunk, not a full chunk followed by an empty one. */
    @Test
    public void validatesExactChunkBoundaryWithoutEmptyTail() {
        final byte[] content = new byte[UploadEvent.MAX_CHUNK_SIZE];

        assertTrue(event("boundary.bin", content.length, content, 0, 1).isValid());
        assertFalse(event("boundary.bin", content.length, new byte[0], 1, 2).isValid());
    }

    /** File names crossing a filesystem boundary are rejected even though they are only metadata. */
    @Test
    public void rejectsPathLikeFileNames() {
        assertFalse(event("../secret.txt", 1, new byte[1], 0, 1).isValid());
        assertFalse(event("C:\\secret.txt", 1, new byte[1], 0, 1).isValid());
        assertTrue(event("safe name.txt", 1, new byte[1], 0, 1).isValid());
    }

    /** Declared geometry must agree with both the index and actual binary length. */
    @Test
    public void rejectsInconsistentChunkGeometry() {
        assertFalse(event("file.bin", 2, new byte[1], 0, 1).isValid());
        assertFalse(event("file.bin", 1, new byte[1], 1, 1).isValid());
        assertFalse(event("file.bin", -1, new byte[0], 0, 1).isValid());
    }

    private static UploadEvent event(final String name, final int size, final byte[] content,
            final int chunkIndex, final int totalChunks) {
        return new UploadEvent(
            1,
            name,
            "application/octet-stream",
            size,
            content,
            chunkIndex,
            totalChunks
        );
    }
}
