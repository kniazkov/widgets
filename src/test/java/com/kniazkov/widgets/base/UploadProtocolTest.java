/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.widgets.common.RMId;
import com.kniazkov.widgets.common.UploadProtocol;
import com.kniazkov.widgets.view.FileLoader;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * Tests upload-protocol values shared across framework layers.
 */
public final class UploadProtocolTest {
    /**
     * Every rejected path must return one singleton acknowledgement.
     */
    @Test
    public void sharesRejectedResponseAcrossLayers() {
        final Application application = BaseTestSupport.application((root, context) -> { });
        final Client client = new Client();
        final FileLoader loader = new FileLoader();

        assertSame(
            UploadProtocol.rejected(),
            application.uploadChunk(RMId.create(), RMId.create(), 1, 0, new byte[0], "#0")
        );
        assertSame(
            UploadProtocol.rejected(),
            client.uploadChunk(RMId.create(), 1, 0, new byte[0], "#0")
        );
        assertSame(
            UploadProtocol.rejected(),
            loader.handleUploadChunk(1, 0, new byte[0])
        );
    }
}
