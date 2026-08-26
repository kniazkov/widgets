/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.webserver.ErrorPage;
import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.SslOptions;
import java.io.File;
import java.net.InetAddress;
import java.time.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Tests the complete widgets-to-webserver 2.0 configuration bridge. */
public class ServerOptionsTest {
    /** Temporary TLS identity placeholder accepted by the immutable SSL builder. */
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    /** Every exposed listener, limit, timeout, handler, and HTTPS option must be forwarded. */
    @Test
    public void forwardsAllWebServerOptions() throws Exception {
        final File keyStore = this.folder.newFile("server.p12");
        final char[] password = "changeit".toCharArray();
        final SslOptions sslOptions = new SslOptions.Builder()
            .setKeyStoreFile(keyStore)
            .setPassword(password)
            .build();
        final Handler handler = (request, environment) ->
            environment.getResponseFactory().notFound();
        final ErrorPage errorPage = (code, reason, message) -> "custom error";
        final Options source = new Options();
        source.port = 0;
        source.bindAddress = InetAddress.getLoopbackAddress();
        source.backlog = 17;
        source.wwwRoot = "public";
        source.maxRequestSize = 1001;
        source.maxFileSize = 1002;
        source.maxInMemoryBodySize = 1003;
        source.maxFormSize = 1004;
        source.maxMultipartParts = 19;
        source.maxMultipartHeaderSize = 1005;
        source.maxHeaderSize = 1006;
        source.maxWorkers = 23;
        source.readTimeout = Duration.ofSeconds(7);
        source.writeTimeout = Duration.ofSeconds(8);
        source.handlerTimeout = Duration.ofSeconds(9);
        source.errorPage = errorPage;
        source.sslOptions = sslOptions;

        final com.kniazkov.webserver.Options actual = Server.getWebServerOptions(
            source.clone(),
            handler
        );

        assertEquals(0, actual.getPort());
        assertEquals(source.bindAddress, actual.getBindAddress().orElse(null));
        assertEquals(17, actual.getBacklog());
        assertEquals("public", actual.getWwwRoot());
        assertEquals(1001, actual.getMaxRequestSize());
        assertEquals(1002, actual.getMaxFileSize());
        assertEquals(1003, actual.getMaxInMemoryBodySize());
        assertEquals(1004, actual.getMaxFormSize());
        assertEquals(19, actual.getMaxMultipartParts());
        assertEquals(1005, actual.getMaxMultipartHeaderSize());
        assertEquals(1006, actual.getMaxHeaderSize());
        assertEquals(23, actual.getMaxWorkers());
        assertEquals(Duration.ofSeconds(7), actual.getReadTimeout());
        assertEquals(Duration.ofSeconds(8), actual.getWriteTimeout());
        assertEquals(Duration.ofSeconds(9), actual.getHandlerTimeout());
        assertSame(errorPage, actual.getErrorPage());
        assertSame(handler, actual.getHandler());
        assertTrue(actual.getSslOptions().isPresent());
        assertSame(sslOptions, actual.getSslOptions().get());
    }
}
