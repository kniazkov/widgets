/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.Section;
import java.net.InetAddress;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

/**
 * Uses real loopback HTTP to verify legal multipart upload sizes and boundary responses.
 */
public class UploadMultipartLimitsTest {
    /**
     * Configured hard maximum size of a binary upload chunk.
     */
    private static final int MAX = 1024 * 1024;

    /**
     * Tests the maximum chunk, including a genuinely larger complete HTTP body.
     */
    @Test
    public void acceptsFullMaximumChunkWithOrdinaryMultipartEnvelope() throws Exception {
        try (Fixture fixture = new Fixture(MAX, MAX)) {
            final String name = "l".repeat(251) + ".bin";
            fixture.select(1, name, MAX, 1);
            final String full = fixture.body(1, 0, MAX, name);
            assertTrue(full.length() > MAX);
            final HttpResponse<String> response = fixture.send(full);
            assertEquals(200, response.statusCode());
            assertTrue(ProtocolTestBrowser.json(response).get("complete").booleanValue());
            assertEquals(name + ":" + MAX, fixture.loaded());
            assertEquals(413, fixture.send(fixture.body(2, 0, MAX + 1, "larger.bin"))
                .statusCode());
        }
    }

    /**
     * Covers a short file, a boundary chunk, a short final chunk and a clear excess rejection.
     */
    @Test
    public void distinguishesChunkAndCompleteFileLimits() throws Exception {
        final int chunk = 64;
        try (Fixture fixture = new Fixture(chunk, chunk + 3)) {
            fixture.select(1, "under.bin", chunk - 1, 1);
            assertTrue(fixture.chunk(1, 0, chunk - 1).get("complete").booleanValue());
            assertEquals("under.bin:63", fixture.loaded());

            fixture.select(2, "multichunk.bin", chunk + 3, 2);
            final JsonObject excess = fixture.chunk(2, 0, chunk + 1);
            assertFalse(excess.get("result").booleanValue());
            assertEquals("chunkTooLarge", excess.get("uploadError").stringValue());
            assertEquals("64", excess.get("limitBytes").toString());
            final JsonObject initial = fixture.chunk(2, 0, chunk);
            assertTrue(initial.get("result").booleanValue());
            assertFalse(initial.get("complete").booleanValue());
            assertTrue(fixture.chunk(2, 1, 3).get("complete").booleanValue());
            assertEquals("multichunk.bin:67", fixture.loaded());

            fixture.select(3, "file-limit.bin", chunk + 4, 2);
            assertFalse(fixture.chunk(3, 0, chunk).get("result").booleanValue());
            assertFalse(fixture.selectedNames.contains("file-limit.bin"));
        }
    }

    /**
     * Chunk size remains bounded even though the wire has an envelope allowance.
     */
    @Test
    public void rejectsImpossibleConfigurationButAllowsShortFiles() throws Exception {
        assertThrows(IllegalArgumentException.class,
            () -> new Options.Builder().setChunkSize(MAX + 1));
        assertThrows(IllegalArgumentException.class, () -> new Options.Builder().setChunkSize(0));
        try (Fixture fixture = new Fixture(MAX, 5)) {
            fixture.select(1, "tiny.bin", 5, 1);
            assertTrue(fixture.chunk(1, 0, 5).get("complete").booleanValue());
            assertEquals("tiny.bin:5", fixture.loaded());
        }
    }

    /**
     * Isolated authorized browser transport and file loader for the upload tests.
     */
    private static final class Fixture implements AutoCloseable {
        /**
         * Standard boundary for benign test bodies.
         */
        private static final String BOUNDARY = "widgets-upload-limit-test";

        /**
         * Local listener.
         */
        private final com.kniazkov.webserver.Server server;

        /**
         * Loopback HTTP browser.
         */
        private final ProtocolTestBrowser browser;

        /**
         * Newly negotiated channel.
         */
        private final JsonObject channel;

        /**
         * Loader ID assigned by the page factory.
         */
        private final AtomicReference<String> widgetId = new AtomicReference<>();

        /**
         * Ordinary selections observed by the application.
         */
        private final LinkedBlockingQueue<String> selectedNames = new LinkedBlockingQueue<>();

        /**
         * Successful file deliveries.
         */
        private final LinkedBlockingQueue<String> completions = new LinkedBlockingQueue<>();

        /**
         * @param chunk maximum binary payload bytes
         * @param file maximum whole-file bytes
         */
        Fixture(final int chunk, final int file) throws Exception {
            final Application app = new Application((root, context) -> {
                final FileLoader loader = new FileLoader("Select a fixture");
                this.widgetId.set(loader.getId().toString());
                loader.onSelect(upload -> {
                    this.selectedNames.add(upload.getName());
                    upload.onLoad(done -> this.completions
                        .add(done.getName() + ":" + done.getSize()));
                });
                root.add(new Section(loader));
            });
            this.server = Server.start(app, new Options.Builder()
                .setBindAddress(InetAddress.getLoopbackAddress()).setPort(0)
                .setChunkSize(chunk).setMaxFileSize(file).build());
            try {
                this.browser = new ProtocolTestBrowser(this.server.getPort());
                this.channel = this.browser.create(null);
            } catch (final Exception failure) {
                this.server.stop();
                throw failure;
            }
        }

        /**
         * @param id browser-local file/event ID
         * @param name ordinary file name
         * @param size selected file size
         * @param count announced number of chunks
         */
        void select(final int id, final String name, final int size, final int count)
                throws Exception {
            final JsonArray events = new JsonArray();
            final JsonObject event = events.createObject();
            event.addString("id", "#" + id);
            event.addString("widget", this.widgetId.get());
            event.addString("type", "upload");
            final JsonObject data = event.createObject("data");
            data.addNumber("fileId", id);
            data.addString("name", name);
            data.addString("type", "application/octet-stream");
            data.addNumber("size", size);
            data.addNumber("totalChunks", count);
            final var response = this.browser.post(Map.of("action", "synchronize",
                "client", this.channel.get("id").stringValue(), "events", events.toString()),
                this.browser.headers(this.channel));
            assertEquals(200, response.statusCode());
        }

        /**
         * @param id browser-local selected file
         * @param index chunk index
         * @param bytes binary payload size
         * @return parsed ordinary acknowledgement
         */
        JsonObject chunk(final int id, final int index, final int bytes) throws Exception {
            final HttpResponse<String> response = this.send(this.body(id, index, bytes,
                "chunk.bin"));
            assertEquals(200, response.statusCode());
            return ProtocolTestBrowser.json(response);
        }

        /**
         * @param id selected file
         * @param index chunk index
         * @param bytes count of ordinary payload bytes
         * @param name multipart part filename
         * @return ASCII test body with standard multipart framing
         */
        String body(final int id, final int index, final int bytes, final String name) {
            final StringBuilder body = new StringBuilder();
            Map.of("action", "upload chunk", "client", this.channel.get("id").stringValue(),
                "widget", this.widgetId.get(), "fileId", Integer.toString(id),
                "chunkIndex", Integer.toString(index), "lastUpdate", "#0")
                .forEach((key, value) -> body.append("--").append(BOUNDARY)
                    .append("\r\nContent-Disposition: form-data; name=\"").append(key)
                    .append("\"\r\n\r\n").append(value).append("\r\n"));
            return body.append("--").append(BOUNDARY)
                .append("\r\nContent-Disposition: form-data; name=\"chunk\"; filename=\"")
                .append(name).append("\"\r\nContent-Type: application/octet-stream\r\n\r\n")
                .append("x".repeat(bytes)).append("\r\n--").append(BOUNDARY).append("--\r\n")
                .toString();
        }

        /**
         * @param body ordinary multipart request
         * @return real HTTP response
         */
        HttpResponse<String> send(final String body) throws Exception {
            return this.browser.send("multipart/form-data; boundary=" + BOUNDARY, body,
                this.browser.headers(this.channel));
        }

        /**
         * @return completed callback's name and size
         */
        String loaded() throws InterruptedException {
            final String value = this.completions.poll(5, TimeUnit.SECONDS);
            assertNotNull("Application did not receive completed file", value);
            return value;
        }

        @Override
        public void close() throws com.kniazkov.webserver.ServerException {
            this.server.stop();
        }
    }
}
