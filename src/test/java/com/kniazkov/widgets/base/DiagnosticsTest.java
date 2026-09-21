/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Verifies actual log records and propagation at the HTTP boundary.
 */
public final class DiagnosticsTest {
    /**
     * Page factory failures produce HTTP 500 and one record holding the original throwable.
     */
    @Test
    public void logsPageFailureOnce() throws Exception {
        final RuntimeException failure = new IllegalStateException("private-test-message");
        final Application application = new Application((root, context) -> {
            throw failure;
        });
        final Capture capture = new Capture();
        final Logger logger = Logger.getLogger(HttpHandler.class.getName());
        logger.addHandler(capture);
        final com.kniazkov.webserver.Server server = Server.start(application,
            new Options.Builder().setPort(0).build());
        try {
            final HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort()
                    + "/?action=new%20instance&address=/&mobile=false"
                    + "&browserId=00000000-0000-0000-0000-000000000001")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(500, response.statusCode());
            assertFalse(response.body().contains("private-test-message"));
            assertEquals(1, capture.records.size());
            assertSame(failure, capture.records.get(0).getThrown());
        } finally {
            server.stop();
            logger.removeHandler(capture);
        }
    }

    /**
     * Failures before action dispatch are logged and rethrown, not swallowed.
     */
    @Test
    public void logsBoundaryFailureAndRethrowsSameInstance() {
        final Capture capture = new Capture();
        final Logger logger = Logger.getLogger(HttpHandler.class.getName());
        logger.addHandler(capture);
        try {
            final HttpHandler handler = new HttpHandler(BaseTestSupport.application((r, c) -> { }),
                new Options.Builder().build());
            final NullPointerException failure = assertThrows(NullPointerException.class,
                () -> handler.handle(null, null));
            assertEquals(1, capture.records.size());
            assertSame(failure, capture.records.get(0).getThrown());
        } finally {
            logger.removeHandler(capture);
        }
    }

    /**
     * Browser reports require a live client, are bounded and cannot forge multiline logs.
     */
    @Test
    public void boundsAndDeduplicatesBrowserReports() throws Exception {
        final Application app = BaseTestSupport.application((root, context) -> { });
        final Capture capture = new Capture();
        final Logger logger = Logger.getLogger(Application.class.getName());
        logger.addHandler(capture);
        try {
            app.reportBrowserError(Map.of("client", "invalid", "error", "ignored"));
            final var id = app.createClient("/", new PageContext());
            app.reportBrowserError(Map.of("client", id.toString(),
                "error", "boom\n" + "x".repeat(9000)));
            app.reportBrowserError(Map.of("client", id.toString(), "error", "duplicate"));
            assertEquals(1, capture.records.size());
            final String message = capture.records.get(0).getMessage();
            assertTrue(message.contains("boom "));
            assertFalse(message.contains("\n"));
            assertTrue(message.length() < 8500);
        } finally {
            logger.removeHandler(capture);
        }
    }

    /**
     * In-memory JUL handler.
     */
    private static final class Capture extends Handler {
        /**
         * Captured records.
         */
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            this.records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
