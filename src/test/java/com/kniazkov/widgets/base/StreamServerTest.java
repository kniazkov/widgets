/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

/**
 * Real native upgrades, authenticated callbacks and lifecycle budgets.
 */
public final class StreamServerTest {
    /**
     * Cookie + AUTH establish a real stream; peer close returns all reservations.
     */
    @Test
    public void authenticatesAndReleasesAfterPeerClose() throws Exception {
        try (Fixture fixture = new Fixture((connection, text) -> connection.send(text))) {
            final Peer peer = fixture.connect();
            fixture.authenticate(peer);
            peer.socket.sendText("hello", true).join();
            assertEquals("hello", peer.messages.poll(3, TimeUnit.SECONDS));
            peer.socket.sendClose(1000, "done").join();
            fixture.awaitEmpty();
        }
    }

    /**
     * Upgrade metadata and first-frame authentication reject untrusted callers.
     */
    @Test
    public void refusesUntrustedUpgradesAndInvalidAuth() throws Exception {
        try (Fixture fixture = new Fixture((connection, text) -> fail("Unauthorized callback"))) {
            assertThrows(Exception.class, () -> fixture.connect("https://evil.example", true));
            assertThrows(Exception.class, () -> fixture.connect(fixture.origin, false));
            for (final String auth : new String[]{"{}", fixture.auth().replace("auth", "echo"),
                    fixture.auth().replace(fixture.owner.csrf(), "wrong"),
                    fixture.auth().replace("\"version\":1", "\"version\":1.5")}) {
                final Peer peer = fixture.connect();
                peer.socket.sendText(auth, true).join();
                assertTrue(peer.closed.await(3, TimeUnit.SECONDS));
                fixture.awaitEmpty();
            }
        }
    }

    /**
     * Authentication deadline and owner revocation both reclaim sockets.
     */
    @Test
    public void expiresUnauthenticatedConnectionsAndRevokedOwners() throws Exception {
        try (Fixture fixture = new Fixture((connection, text) -> connection.send(text))) {
            final Peer silent = fixture.connect();
            assertTrue(silent.closed.await(3, TimeUnit.SECONDS));
            fixture.awaitEmpty();
            final Peer peer = fixture.connect();
            fixture.authenticate(peer);
            fixture.owner.revoke();
            assertTrue(peer.closed.await(3, TimeUnit.SECONDS));
            fixture.awaitEmpty();
        }
    }

    /**
     * Stuck user callbacks are interrupted while ordinary HTTP remains available.
     */
    @Test
    public void cancelsStuckCallbackWithoutBlockingHttp() throws Exception {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch interrupted = new CountDownLatch(1);
        try (Fixture fixture = new Fixture((connection, text) -> {
            entered.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (final InterruptedException stopped) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
            }
        })) {
            final Peer peer = fixture.connect();
            fixture.authenticate(peer);
            peer.socket.sendText("hold", true).join();
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            final var response = fixture.client.send(java.net.http.HttpRequest.newBuilder(
                URI.create(fixture.origin + "/")).timeout(Duration.ofSeconds(2)).build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertTrue(interrupted.await(3, TimeUnit.SECONDS));
            fixture.awaitEmpty();
        }
    }

    /**
     * Concurrent upgrade limits are hard bounds and application close owns cleanup.
     */
    @Test
    public void limitsConnectionsAndClosesOnApplicationShutdown() throws Exception {
        try (Fixture fixture = new Fixture((connection, text) -> connection.send(text))) {
            final Peer first = fixture.connect();
            fixture.authenticate(first);
            final Peer second = fixture.connect();
            fixture.authenticate(second);
            assertThrows(Exception.class, fixture::connect);
            fixture.app.close();
            assertTrue(first.closed.await(3, TimeUnit.SECONDS));
            assertTrue(second.closed.await(3, TimeUnit.SECONDS));
            fixture.awaitEmpty();
        }
    }

    /**
     * Binary payloads stay on HTTP and text frame size is bounded.
     */
    @Test
    public void rejectsBinaryAndOversizedMessages() throws Exception {
        try (Fixture fixture = new Fixture((connection, text) -> fail("Unexpected callback"))) {
            final Peer binary = fixture.connect();
            fixture.authenticate(binary);
            binary.socket.sendBinary(java.nio.ByteBuffer.wrap(new byte[]{1}), true).join();
            assertTrue(binary.closed.await(3, TimeUnit.SECONDS));
            fixture.awaitEmpty();
            final Peer large = fixture.connect();
            fixture.authenticate(large);
            large.socket.sendText("x".repeat(2048), true).join();
            assertTrue(large.closed.await(3, TimeUnit.SECONDS));
            fixture.awaitEmpty();
        }
    }

    /**
     * A full executor rejects another callback instead of extending its queue indefinitely.
     */
    @Test
    public void boundsApplicationCallbackQueue() throws Exception {
        final CountDownLatch entered = new CountDownLatch(1);
        try (Fixture fixture = new Fixture((connection, text) -> {
            entered.countDown();
            new CountDownLatch(1).await();
        }, 4)) {
            final Peer running = fixture.connect();
            fixture.authenticate(running);
            final Peer queued = fixture.connect();
            fixture.authenticate(queued);
            final Peer rejected = fixture.connect();
            fixture.authenticate(rejected);
            running.socket.sendText("hold", true).join();
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            queued.socket.sendText("hold", true).join();
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (fixture.stream.getMetrics().get("queuedHandlers") != 1
                    && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }
            assertEquals(Long.valueOf(1), fixture.stream.getMetrics().get("queuedHandlers"));
            rejected.socket.sendText("overflow", true).join();
            assertTrue(rejected.closed.await(3, TimeUnit.SECONDS));
            assertEquals(1013, rejected.status);
            fixture.awaitEmpty();
        }
    }

    /**
     * An incomplete native write remains charged to both queue budgets until completion.
     */
    @Test
    public void boundsSlowWriterAndReleasesQueueOnOverflow() throws Exception {
        try (Fixture fixture = new Fixture((connection, text) -> { })) {
            final StreamEndpoint endpoint = new StreamEndpoint(fixture.stream, fixture.owner);
            final var writes = new java.util.ArrayList<String>();
            final var callbacks =
                new java.util.ArrayList<org.eclipse.jetty.websocket.api.Callback>();
            final var socket = (org.eclipse.jetty.websocket.api.Session)
                java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{org.eclipse.jetty.websocket.api.Session.class},
                    (proxy, method, arguments) -> {
                        if ("sendText".equals(method.getName())) {
                            writes.add((String) arguments[0]);
                            callbacks.add((org.eclipse.jetty.websocket.api.Callback) arguments[1]);
                        }
                        return null;
                    });
            endpoint.onWebSocketOpen(socket);
            endpoint.onWebSocketText(fixture.auth());
            callbacks.removeFirst().succeed();
            assertTrue(endpoint.send("я".repeat(512)));
            assertTrue(endpoint.send("x".repeat(1024)));
            assertEquals(2, endpoint.queued()[0]);
            assertEquals(2048, endpoint.queued()[1]);
            assertEquals(2, writes.size());
            assertTrue(!endpoint.send("overflow"));
            assertEquals(0, endpoint.queued()[0]);
            assertEquals(0, endpoint.queued()[1]);
            callbacks.removeFirst().succeed();
            assertTrue(!endpoint.isOpen());
        }
    }

    /**
     * A throwing consumer cannot leak its connection or worker reservation.
     */
    @Test
    public void handlerFailureClosesConnection() throws Exception {
        try (Fixture fixture = new Fixture((connection, text) -> {
            throw new IllegalStateException("fixture failure");
        })) {
            final Peer peer = fixture.connect();
            fixture.authenticate(peer);
            peer.socket.sendText("fail", true).join();
            assertTrue(peer.closed.await(3, TimeUnit.SECONDS));
            fixture.awaitEmpty();
        }
    }

    /**
     * Insecure remote origins and impossible budgets fail before opening a listener.
     */
    @Test
    public void validatesConfiguration() {
        assertThrows(IllegalArgumentException.class,
            () -> StreamOptions.defaults(0, "http://example.com"));
        assertThrows(IllegalArgumentException.class,
            () -> StreamOptions.defaults(-1, "https://example.com"));
        assertThrows(IllegalArgumentException.class,
            () -> new StreamOptions(0, "https://example.com", 1, 1, 1, 1024, 1, 512,
                1000, 1000, 2000));
        try (Application app = new Application((root, context) -> { })) {
            assertThrows(IllegalStateException.class, () -> StreamServer.start(app,
                StreamOptions.defaults(0, "https://example.com"), (connection, text) -> { }));
        }
    }

    /**
     * Isolated real HTTP and WebSocket listeners sharing one browser owner.
     */
    private static final class Fixture implements AutoCloseable {
        /**
         * Application owning both listeners.
         */
        private final Application app = new Application((root, context) -> { });
        /**
         * Real HTTP/WebSocket client.
         */
        private final HttpClient client = HttpClient.newHttpClient();
        /**
         * Cookie owner used by test peers.
         */
        private final BrowserSession owner;
        /**
         * HTTP origin accepted by the listener.
         */
        private final String origin;
        /**
         * Independent stream adapter.
         */
        private final StreamServer stream;

        /**
         * Creates listeners and bounded test budgets.
         */
        Fixture(final StreamHandler handler) {
            this(handler, 2);
        }

        /**
         * Creates a connection budget with one worker and one queue slot.
         */
        Fixture(final StreamHandler handler, final int connections) {
            final var http = Server.start(this.app, new Options.Builder().setPort(0).build());
            this.origin = "http://127.0.0.1:" + http.getPort();
            this.owner = this.app.bootstrapSession(null);
            this.stream = StreamServer.start(this.app, new StreamOptions(0, this.origin,
                connections, 1, 1, 1024, 2, 2048, 500, 200, 1000), handler);
        }

        /**
         * Connects with valid same-origin credentials.
         */
        Peer connect() throws Exception {
            return this.connect(this.origin, true);
        }

        /**
         * Connects with explicit untrusted upgrade metadata.
         */
        Peer connect(final String origin, final boolean cookie) throws Exception {
            final Peer peer = new Peer();
            final var builder = this.client.newWebSocketBuilder().header("Origin", origin)
                .connectTimeout(Duration.ofSeconds(3)).subprotocols(StreamServer.SUBPROTOCOL);
            if (cookie) {
                builder.header("Cookie", "widgets-session=" + this.owner.credential());
            }
            peer.socket = builder.buildAsync(URI.create("ws://127.0.0.1:"
                + this.stream.getPort() + StreamServer.PATH), peer).get(3, TimeUnit.SECONDS);
            return peer;
        }

        /**
         * Sends the first AUTH message and waits for readiness.
         */
        void authenticate(final Peer peer) throws Exception {
            peer.socket.sendText(this.auth(), true).join();
            final String ready = peer.messages.poll(3, TimeUnit.SECONDS);
            assertNotNull(ready);
            assertTrue(ready.contains("\"type\":\"ready\""));
        }

        /**
         * @return first frame using this fixture's bootstrap credentials
         */
        String auth() {
            return "{\"type\":\"auth\",\"version\":1,\"session\":\""
                + this.owner.getId() + "\",\"csrf\":\"" + this.owner.csrf() + "\"}";
        }

        /**
         * Waits for resource release without assuming callback ordering.
         */
        void awaitEmpty() throws Exception {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (this.stream.getMetrics().values().stream().anyMatch(value -> value != 0)
                    && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertTrue(this.stream.getMetrics().toString(),
                this.stream.getMetrics().values().stream().allMatch(value -> value == 0));
        }

        @Override
        public void close() {
            this.app.close();
            this.client.close();
        }
    }

    /**
     * Fragment-safe test receiver; the JDK automatically answers protocol pings.
     */
    private static final class Peer implements WebSocket.Listener {
        /**
         * Complete incoming messages.
         */
        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        /**
         * Terminal peer signal.
         */
        private final CountDownLatch closed = new CountDownLatch(1);
        /**
         * Current fragmented text message.
         */
        private final StringBuilder text = new StringBuilder();
        /**
         * Native client socket.
         */
        private WebSocket socket;
        /**
         * Native terminal category for overload assertions.
         */
        private volatile int status;

        @Override
        public void onOpen(final WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(final WebSocket webSocket, final CharSequence data,
                final boolean last) {
            this.text.append(data);
            if (last) {
                this.messages.add(this.text.toString());
                this.text.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(final WebSocket webSocket, final int status,
                final String reason) {
            this.status = status;
            this.closed.countDown();
            return null;
        }

        @Override
        public void onError(final WebSocket webSocket, final Throwable error) {
            this.closed.countDown();
        }
    }
}
