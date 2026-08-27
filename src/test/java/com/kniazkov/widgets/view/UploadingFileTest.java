/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.common.UploadedFile;
import com.kniazkov.widgets.common.UploadProtocol;
import com.kniazkov.widgets.controller.Event;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests binary chunk assembly and acknowledgements.
 */
public final class UploadingFileTest {
    /**
     * Missing and duplicate chunks must be detected without corrupting the completed file.
     */
    @Test
    public void acknowledgesFirstMissingChunkAndAcceptsIdempotentRetries() throws Exception {
        final FileLoader loader = new FileLoader();
        final AtomicReference<UploadingFile> selected = new AtomicReference<>();
        loader.onSelect(selected::set);
        final int chunkSize = 4 * 1024;
        final Options options = new Options.Builder()
            .setChunkSize(chunkSize)
            .setMaxFileSize(3 * chunkSize)
            .build();
        final WidgetSandbox<FileLoader> sandbox = WidgetSandbox.open(loader, options);
        sandbox.clearUpdates();
        final JsonObject selection = selection(17, 2 * chunkSize + 3, 3);

        sandbox.fire(Event.UPLOAD, selection);

        final UploadingFile descriptor = selected.get();
        final CountDownLatch loaded = new CountDownLatch(1);
        final AtomicReference<UploadedFile> result = new AtomicReference<>();
        descriptor.onLoad(file -> {
            result.set(file);
            loaded.countDown();
        });
        final byte[] first = bytes(chunkSize, (byte) 1);
        final byte[] second = bytes(chunkSize, (byte) 2);
        final byte[] third = bytes(3, (byte) 3);

        assertReceipt(loader.handleUploadChunk(17, 2, third), 0, false);
        assertEquals(Integer.valueOf(33), descriptor.getLoadingPercentageModel().getData());
        assertReceipt(loader.handleUploadChunk(17, 2, third), 0, false);
        assertEquals(Integer.valueOf(33), descriptor.getLoadingPercentageModel().getData());
        assertReceipt(loader.handleUploadChunk(17, 0, first), 1, false);
        assertReceipt(loader.handleUploadChunk(17, 1, second), 3, true);

        assertTrue("onLoad was not called", loaded.await(2, TimeUnit.SECONDS));
        final byte[] expected = new byte[first.length + second.length + third.length];
        System.arraycopy(first, 0, expected, 0, first.length);
        System.arraycopy(second, 0, expected, first.length, second.length);
        System.arraycopy(third, 0, expected, first.length + second.length, third.length);
        assertArrayEquals(expected, result.get().getContent());
        assertEquals(Integer.valueOf(100), descriptor.getLoadingPercentageModel().getData());
        assertReceipt(loader.handleUploadChunk(17, 1, second), 3, true);
    }

    /**
     * A changed retry payload must be rejected instead of silently replacing accepted data.
     */
    @Test
    public void rejectsChangedDuplicateChunk() {
        final FileLoader loader = new FileLoader();
        final WidgetSandbox<FileLoader> sandbox = WidgetSandbox.open(loader);
        sandbox.clearUpdates();
        sandbox.fire(Event.UPLOAD, selection(8, 3, 1));

        assertReceipt(loader.handleUploadChunk(8, 0, bytes(3, (byte) 4)), 1, true);

        final JsonObject response = loader.handleUploadChunk(8, 0, bytes(3, (byte) 5));
        assertFalse(response.get("result").getBooleanValue());
    }

    /**
     * Complete-file limits are read from the options retained by the root widget.
     */
    @Test
    public void rejectsSelectionBeyondRootFileLimit() {
        final FileLoader loader = new FileLoader();
        final AtomicReference<UploadingFile> selected = new AtomicReference<>();
        loader.onSelect(selected::set);
        final Options options = new Options.Builder()
            .setChunkSize(4 * 1024)
            .setMaxFileSize(3)
            .build();
        final WidgetSandbox<FileLoader> sandbox = WidgetSandbox.open(loader, options);

        sandbox.fire(Event.UPLOAD, selection(18, 4, 1));

        assertNull(selected.get());
    }

    /**
     * All rejected uploads must reuse one standard response object.
     */
    @Test
    public void reusesRejectedUploadResponse() {
        final FileLoader loader = new FileLoader();

        assertSame(
            loader.handleUploadChunk(91, 0, new byte[0]),
            loader.handleUploadChunk(92, 0, new byte[0])
        );
    }

    /**
     * Creates valid upload-selection metadata.
     */
    private static JsonObject selection(final int id, final int size, final int chunks) {
        final JsonObject data = new JsonObject();
        data.addNumber("fileId", id);
        data.addString("name", "data.bin");
        data.addString("type", "application/octet-stream");
        data.addNumber("size", size);
        data.addNumber("totalChunks", chunks);
        return data;
    }

    /**
     * Creates a byte array filled with one value.
     */
    private static byte[] bytes(final int size, final byte value) {
        final byte[] result = new byte[size];
        Arrays.fill(result, value);
        return result;
    }

    /**
     * Checks one serialized upload acknowledgement.
     */
    private static void assertReceipt(
            final JsonObject response,
            final int nextChunk,
            final boolean complete) {
        assertTrue(response.get("result").getBooleanValue());
        assertEquals(nextChunk, response.get("nextChunk").getLongValue());
        assertEquals(complete, response.get("complete").getBooleanValue());
    }
}
