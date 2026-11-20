/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.protocol.DownloadFile;
import java.util.Base64;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Tests file transfers scoped to the current root widget.
 */
public final class DownloadFileTest {
    /**
     * The array is snapshotted before sending, and cloning preserves its content.
     */
    @Test
    public void snapshotsFileAndSerializesCommand() {
        final byte[] bytes = {1, 2, 3};
        final DownloadFile command = new DownloadFile("invoice.pdf", "application/pdf", bytes);
        bytes[0] = 0;
        final JsonObject data = new JsonObject();
        command.clone().serialize(data);
        assertEquals("download file", data.get("action").stringValue());
        assertEquals("invoice.pdf", data.get("name").stringValue());
        assertArrayEquals(new byte[]{1, 2, 3},
            Base64.getDecoder().decode(data.get("content").stringValue()));
        final RootWidget root = new RootWidget();
        final var sandbox = WidgetSandbox.open(root);
        sandbox.clearUpdates();
        root.downloadFile("invoice.pdf", "application/pdf", bytes);
        assertEquals(1, WidgetSandbox.findUpdates(sandbox.drainUpdates(), "download file").size());
    }

    /**
     * Rejects paths, MIME types with parameters, empty files, and oversized files.
     */
    @Test
    public void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class,
            () -> new DownloadFile("../file", "application/pdf", new byte[]{1}));
        assertThrows(IllegalArgumentException.class,
            () -> new DownloadFile("file", "application/pdf\r\nX-Test: true", new byte[]{1}));
        assertThrows(IllegalArgumentException.class,
            () -> new DownloadFile("file", "application/pdf", new byte[0]));
        assertThrows(IllegalArgumentException.class,
            () -> new DownloadFile("file", "application/pdf", new byte[5 * 1024 * 1024 + 1]));
    }
}
