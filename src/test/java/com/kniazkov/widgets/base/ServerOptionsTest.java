/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.SslOptions;
import com.kniazkov.widgets.view.RootWidget;
import java.io.File;
import java.net.InetAddress;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests immutable application options and the intentionally narrow webserver bridge.
 */
public class ServerOptionsTest {
    /**
     * Temporary TLS identity placeholder accepted by the immutable SSL builder.
     */
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    /**
     * Public listener settings are forwarded while HTTP mechanics use framework constants.
     */
    @Test
    public void buildsWebServerOptions() throws Exception {
        final File keyStore = this.folder.newFile("server.p12");
        final SslOptions sslOptions = new SslOptions.Builder()
            .setKeyStoreFile(keyStore)
            .setPassword("changeit".toCharArray())
            .build();
        final Handler handler = (request, environment) ->
            environment.getResponseFactory().notFound();
        final Options source = new Options.Builder()
            .setPort(0)
            .setBindAddress(InetAddress.getLoopbackAddress())
            .setMaxWorkers(23)
            .setSslOptions(sslOptions)
            .build();

        final com.kniazkov.webserver.Options actual = Server.getWebServerOptions(
            source,
            handler
        );

        assertEquals(0, actual.getPort());
        assertEquals(source.getBindAddress().orElse(null), actual.getBindAddress().orElse(null));
        assertEquals(23, actual.getMaxWorkers());
        assertEquals(WebServerDefaults.BACKLOG, actual.getBacklog());
        assertEquals(WebServerDefaults.MAX_REQUEST_SIZE, actual.getMaxRequestSize());
        assertEquals(WebServerDefaults.MAX_FILE_SIZE, actual.getMaxFileSize());
        assertEquals(
            WebServerDefaults.MAX_IN_MEMORY_BODY_SIZE,
            actual.getMaxInMemoryBodySize()
        );
        assertEquals(WebServerDefaults.MAX_FORM_SIZE, actual.getMaxFormSize());
        assertEquals(WebServerDefaults.MAX_MULTIPART_PARTS, actual.getMaxMultipartParts());
        assertEquals(
            WebServerDefaults.MAX_MULTIPART_HEADER_SIZE,
            actual.getMaxMultipartHeaderSize()
        );
        assertEquals(WebServerDefaults.MAX_HEADER_SIZE, actual.getMaxHeaderSize());
        assertEquals(WebServerDefaults.READ_TIMEOUT, actual.getReadTimeout());
        assertEquals(WebServerDefaults.WRITE_TIMEOUT, actual.getWriteTimeout());
        assertEquals(WebServerDefaults.HANDLER_TIMEOUT, actual.getHandlerTimeout());
        assertSame(handler, actual.getHandler());
        assertTrue(actual.getSslOptions().isPresent());
        assertSame(sslOptions, actual.getSslOptions().get());
    }

    /**
     * Building again after changing the builder cannot mutate an earlier options snapshot.
     */
    @Test
    public void builderCreatesIndependentSnapshots() {
        final Options.Builder builder = new Options.Builder()
            .setClientLifetime(1000)
            .setWwwRoot("first")
            .setPort(8001)
            .setMaxWorkers(7)
            .setChunkSize(4096)
            .setMaxFileSize(100_000)
            .setDebug(false);
        final Options first = builder.build();

        final Options second = builder
            .setClientLifetime(2000)
            .setWwwRoot("second")
            .setPort(8002)
            .setMaxWorkers(8)
            .setChunkSize(8192)
            .setMaxFileSize(200_000)
            .setDebug(true)
            .build();

        assertEquals(1000, first.getClientLifetime());
        assertEquals("first", first.getWwwRoot());
        assertEquals(8001, first.getPort());
        assertEquals(7, first.getMaxWorkers());
        assertEquals(4096, first.getChunkSize());
        assertEquals(100_000, first.getMaxFileSize());
        assertFalse(first.isDebug());
        assertEquals(2000, second.getClientLifetime());
        assertEquals("second", second.getWwwRoot());
        assertEquals(8002, second.getPort());
        assertEquals(8, second.getMaxWorkers());
        assertEquals(8192, second.getChunkSize());
        assertEquals(200_000, second.getMaxFileSize());
        assertTrue(second.isDebug());
    }

    /**
     * Upload defaults live in application options and the root retains the same snapshot.
     */
    @Test
    public void rootWidgetRetainsUploadOptions() {
        final Options options = new Options.Builder().build();
        final RootWidget root = new RootWidget(options);

        assertEquals(64 * 1024, options.getChunkSize());
        assertEquals(128 * 1024 * 1024, options.getMaxFileSize());
        assertSame(options, root.getOptions());
    }
}
