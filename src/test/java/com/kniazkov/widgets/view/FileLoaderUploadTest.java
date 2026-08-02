/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.UploadedFile;
import com.kniazkov.widgets.controller.UploadEvent;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Tests ordered, idempotent server-side assembly of uploaded binary chunks. */
public class FileLoaderUploadTest {
    /** Binary bytes are assembled exactly once even when an acknowledgement is retried. */
    @Test
    public void assemblesSequentialChunksAndIgnoresRetries() throws Exception {
        final FileLoader loader = new FileLoader("Upload");
        new RootWidget(new Section(loader));
        final AtomicInteger selections = new AtomicInteger();
        final AtomicReference<UploadedFile> result = new AtomicReference<>();
        final CountDownLatch loaded = new CountDownLatch(1);
        loader.onSelect(upload -> {
            selections.incrementAndGet();
            upload.onLoad(file -> {
                result.set(file);
                loaded.countDown();
            });
        });

        final byte[] first = new byte[UploadEvent.MAX_CHUNK_SIZE];
        for (int index = 0; index < first.length; index++) {
            first[index] = (byte) index;
        }
        final byte[] second = new byte[] {0, (byte) 0xff, 17};
        final int size = first.length + second.length;
        final UploadEvent chunk0 = event(7, "binary.bin", size, first, 0, 2);
        final UploadEvent chunk1 = event(7, "binary.bin", size, second, 1, 2);

        assertTrue(loader.handleUploadEvent(chunk0));
        assertTrue("Repeated accepted chunk must be idempotent", loader.handleUploadEvent(chunk0));
        assertTrue(loader.handleUploadEvent(chunk1));
        assertTrue("Lost final acknowledgement must be safe to retry", loader.handleUploadEvent(chunk1));
        assertTrue(loaded.await(2, TimeUnit.SECONDS));

        final UploadedFile file = result.get();
        assertNotNull(file);
        final byte[] expected = Arrays.copyOf(first, size);
        System.arraycopy(second, 0, expected, first.length, second.length);
        assertArrayEquals(expected, file.getContent());
        assertEquals(1, selections.get());
    }

    /** A gap or metadata change is rejected without poisoning the active upload. */
    @Test
    public void rejectsOutOfOrderAndChangedMetadata() {
        final FileLoader loader = new FileLoader("Upload");
        final byte[] first = new byte[UploadEvent.MAX_CHUNK_SIZE];
        final byte[] second = new byte[] {1};
        final int size = first.length + second.length;

        assertFalse(loader.handleUploadEvent(event(3, "file.bin", size, second, 1, 2)));
        assertTrue(loader.handleUploadEvent(event(3, "file.bin", size, first, 0, 2)));
        assertFalse(loader.handleUploadEvent(event(3, "other.bin", size, second, 1, 2)));
        assertTrue(loader.handleUploadEvent(event(3, "file.bin", size, second, 1, 2)));
    }

    private static UploadEvent event(final int fileId, final String name, final int size,
            final byte[] content, final int chunkIndex, final int totalChunks) {
        return new UploadEvent(
            fileId,
            name,
            "application/octet-stream",
            size,
            content,
            chunkIndex,
            totalChunks
        );
    }
}
