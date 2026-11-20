/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonObject;
import com.kniazkov.json.JsonParsingMode;
import com.kniazkov.widgets.controller.EventTestData;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The same protocol contract over real HTTP and the authenticated stream handler boundary.
 * Native upgrade/AUTH coverage lives in StreamServerTest and the TLS browser probe.
 */
public final class PushServerTest {
    /**
     * Lost responses replay updates and commands exactly once over either physical transport.
     */
    @Test
    public void sharedReplayContract() throws Exception {
        for (final boolean socket : new boolean[]{false, true}) {
            try (Fixture fixture = new Fixture(65536)) {
                final JsonObject channel = fixture.create();
                final JsonObject initial = fixture.exchange(socket, channel, "[]", "#0");
                final String checkpoint = checkpoint(initial);
                final String event = "[{\"id\":\"#1\",\"widget\":\""
                    + fixture.button.getId() + "\",\"type\":\"click\",\"data\":"
                    + EventTestData.pointer() + "}]";
                final JsonObject first = fixture.exchange(socket, channel, event, checkpoint);
                final JsonObject replay = fixture.exchange(socket, channel, event, checkpoint);
                assertEquals(1, fixture.clicks.get());
                assertEquals(first.get("updates").toString(), replay.get("updates").toString());
                assertEquals("#1", replay.get("lastEvent").stringValue());
                final JsonObject acknowledged = fixture.exchange(socket, channel, "[]",
                    checkpoint(replay));
                assertTrue(acknowledged.get("updates").arrayValueOrThrow().isEmpty());
            }
        }
    }

    /**
     * A WS command whose response was lost can safely be repeated through HTTP fallback.
     */
    @Test
    public void lostStreamAckCanResumeThroughHttp() throws Exception {
        try (Fixture fixture = new Fixture(65536)) {
            final JsonObject channel = fixture.create();
            final String initial = checkpoint(fixture.exchange(true, channel, "[]", "#0"));
            final String event = "[{\"id\":\"#1\",\"widget\":\"" + fixture.button.getId()
                + "\",\"type\":\"click\",\"data\":" + EventTestData.pointer() + "}]";
            fixture.exchange(true, channel, event, initial);
            fixture.connection.close();
            final JsonObject replay = fixture.exchange(false, channel, event, initial);
            assertEquals(1, fixture.clicks.get());
            assertTrue(replay.get("result").booleanValue());
            assertTrue(replay.toString().contains("Count: 1"));
        }
    }

    /**
     * Two cached page channels share a connection but retain independent invalidations/ACKs.
     */
    @Test
    public void independentChannelsAndCoalescedPush() throws Exception {
        try (Fixture fixture = new Fixture(65536)) {
            final JsonObject first = fixture.create();
            fixture.exchange(true, first, "[]", "#0");
            final JsonObject second = fixture.create();
            final String secondAck = checkpoint(fixture.exchange(true, second, "[]", "#0"));
            fixture.connection.messages.clear();
            fixture.text.setText("Background change");
            final JsonObject dirty = fixture.connection.take("dirty");
            assertEquals(second.get("id").stringValue(), dirty.get("client").stringValue());
            fixture.text.setText("Another background change");
            Thread.sleep(250);
            assertTrue(fixture.connection.messages.isEmpty());
            final JsonObject response = fixture.exchange(true, second, "[]", secondAck);
            assertTrue(response.toString().contains("Another background change"));
            fixture.unsubscribe(second);
            fixture.text.setText("Detached subscription");
            Thread.sleep(250);
            assertTrue(fixture.connection.messages.isEmpty());
            assertEquals(Long.valueOf(1), fixture.push.getMetrics().get("subscriptions"));
        }
    }

    /**
     * Cookie ownership and channel tokens are checked before subscribing or invoking handlers.
     */
    @Test
    public void rejectsForeignChannelAndCleansDisconnectedReferences() throws Exception {
        try (Fixture fixture = new Fixture(65536)) {
            final JsonObject channel = fixture.create();
            final BrowserSession outsider = fixture.app.bootstrapSession(null);
            final Connection attacker = new Connection(outsider);
            fixture.push.onText(attacker, fixture.subscription(channel));
            assertFalse(attacker.isOpen());
            assertEquals(0, fixture.clicks.get());
            fixture.connection.close();
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (fixture.push.getMetrics().get("subscriptions") != 0
                    && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertEquals(Long.valueOf(0), fixture.push.getMetrics().get("subscriptions"));
        }
    }

    /**
     * Native frame limits do not truncate large snapshots: HTTP replays the same checkpoint.
     */
    @Test
    public void redirectsOversizedResponseToHttpWithoutAdvancingAck() throws Exception {
        try (Fixture fixture = new Fixture(1024)) {
            final JsonObject channel = fixture.create();
            fixture.text.setText("x".repeat(5000));
            fixture.request(channel, "[]", "#0");
            final JsonObject redirect = fixture.connection.take("http");
            assertEquals(1, redirect.get("id").intValue());
            final JsonObject response = fixture.exchange(false, channel, "[]", "#0");
            assertTrue(response.toString().contains("x".repeat(5000)));
            assertFalse(response.get("updates").arrayValueOrThrow().isEmpty());
        }
    }

    /**
     * A blocked application handler on one page cannot stall a second page on the same socket.
     */
    @Test
    public void slowPageDoesNotSerializeOtherChannels() throws Exception {
        try (Fixture fixture = new Fixture(65536)) {
            final JsonObject slow = fixture.create();
            fixture.exchange(true, slow, "[]", "#0");
            final Button button = fixture.button;
            final CountDownLatch entered = new CountDownLatch(1);
            final CountDownLatch release = new CountDownLatch(1);
            button.onClick(event -> {
                entered.countDown();
                try {
                    release.await();
                } catch (final InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            });
            final JsonObject fast = fixture.create();
            fixture.exchange(true, fast, "[]", "#0");
            try {
                fixture.request(slow, "[{\"id\":\"#1\",\"widget\":\"" + button.getId()
                    + "\",\"type\":\"click\",\"data\":" + EventTestData.pointer() + "}]", "#0");
                assertTrue(entered.await(3, TimeUnit.SECONDS));
                fixture.request(fast, "[]", "#0");
                final JsonObject response = fixture.connection.take("response");
                assertEquals(fixture.requestId, response.get("id").longValue());
                assertTrue(response.get("data").objectValueOrThrow().get("result").booleanValue());
                fixture.connection.close();
                final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (fixture.push.getMetrics().get("pushWorkers") != 0
                        && System.nanoTime() < deadline) {
                    Thread.sleep(10);
                }
                assertEquals(Long.valueOf(0), fixture.push.getMetrics().get("pushWorkers"));
                assertEquals(Long.valueOf(0), fixture.push.getMetrics().get("subscriptions"));
            } finally {
                release.countDown();
            }
        }
    }

    /**
     * An application error after a side effect is terminal; HTTP retry cannot repeat the effect.
     */
    @Test
    public void handlerErrorDoesNotRepeatCommittedAction() throws Exception {
        try (Fixture fixture = new Fixture(65536)) {
            final JsonObject channel = fixture.create();
            fixture.button.onClick(event -> {
                fixture.clicks.incrementAndGet();
                throw new AssertionError("Fixture failure after side effect");
            });
            final String event = "[{\"id\":\"#1\",\"widget\":\"" + fixture.button.getId()
                + "\",\"type\":\"click\",\"data\":" + EventTestData.pointer() + "}]";
            final JsonObject failed = fixture.exchange(true, channel, event, "#0");
            assertTrue(failed.get("clientError").booleanValue());
            fixture.exchange(false, channel, event, "#0");
            assertEquals(1, fixture.clicks.get());
        }
    }

    /**
     * Disabling the optional listener stops advertising it without stopping HTTP page creation.
     */
    @Test
    public void rollbackLeavesHttpAvailable() throws Exception {
        try (Fixture fixture = new Fixture(65536)) {
            fixture.create();
            fixture.push.close();
            fixture.push.close();
            final JsonObject channel = fixture.browser.create(null);
            assertFalse(channel.containsKey("pushPath"));
            assertTrue(fixture.exchange(false, channel, "[]", "#0").get("result").booleanValue());
        }
    }

    /**
     * @param response successful synchronization reply
     * @return last applied update checkpoint, independent for each page
     */
    private static String checkpoint(final JsonObject response) {
        final var updates = response.get("updates").arrayValueOrThrow();
        return "#" + updates.get(updates.size() - 1).objectValueOrThrow().get("sequence");
    }

    /**
     * Isolated application plus real HTTP bootstrap and an already-authenticated stream peer.
     */
    private static final class Fixture implements AutoCloseable {
        /**
         * Business counter used to detect duplicate commands.
         */
        private final AtomicInteger clicks = new AtomicInteger();
        /**
         * Current fixture control.
         */
        private Button button;
        /**
         * Current page model target.
         */
        private TextWidget text;
        /**
         * Owning application.
         */
        private final Application app;
        /**
         * Push service under test.
         */
        private final PushServer push;
        /**
         * Real cookie/CSRF bootstrap client.
         */
        private final ProtocolTestBrowser browser;
        /**
         * Authenticated native transport boundary.
         */
        private final Connection connection;
        /**
         * Monotonic physical request identity, unrelated to business event IDs.
         */
        private long requestId;

        /**
         * @param messageBytes native envelope budget
         */
        Fixture(final int messageBytes) throws Exception {
            this.app = new Application((root, context) -> {
                final TextWidget label = new TextWidget("Count: 0");
                final Button action = new Button("Increment");
                action.onClick(event -> label.setText("Count: " + this.clicks.incrementAndGet()));
                this.button = action;
                this.text = label;
                root.add(new Section(action, label));
            });
            final var http = Server.start(this.app, new Options.Builder().setPort(0).build());
            this.push = PushServer.start(this.app, new StreamOptions(0,
                "http://localhost:" + http.getPort(), 16, 2, 16, messageBytes, 16,
                messageBytes * 4, 1000, 500, 5000));
            this.browser = new ProtocolTestBrowser(http.getPort());
            this.connection = new Connection(this.app.browserSession(
                this.browser.cookie.substring(this.browser.cookie.indexOf('=') + 1)));
        }

        /**
         * @return a fresh independently authenticated logical page
         */
        JsonObject create() throws Exception {
            final JsonObject channel = this.browser.create(null);
            assertEquals("/widgets-stream", channel.get("pushPath").stringValue());
            this.push.onText(this.connection, this.subscription(channel));
            this.connection.take("subscribed");
            return channel;
        }

        /**
         * @param channel page credentials
         * @return wire channel credentials, never URL parameters
         */
        JsonObject credentials(final JsonObject channel) {
            final JsonObject result = new JsonObject();
            result.addString("token", channel.get("channelToken").stringValue());
            result.addString("epoch", channel.get("serverId").stringValue());
            result.addString("updateEpoch", channel.get("updateEpoch").stringValue());
            return result;
        }

        /**
         * @param channel authorized logical page
         * @return subscription envelope
         */
        String subscription(final JsonObject channel) {
            final JsonObject packet = new JsonObject();
            packet.addString("type", "subscribe");
            packet.addString("client", channel.get("id").stringValue());
            packet.addElement("channel", this.credentials(channel));
            return packet.toString();
        }

        /**
         * @param channel page whose subscription should end
         */
        void unsubscribe(final JsonObject channel) {
            this.push.onText(this.connection, "{\"type\":\"unsubscribe\",\"client\":\""
                + channel.get("id").stringValue() + "\"}");
        }

        /**
         * @param socket whether to use the native handler or HTTP
         * @param channel logical page
         * @param events unchanged event batch
         * @param checkpoint unchanged replay position
         * @return shared protocol response
         */
        JsonObject exchange(final boolean socket, final JsonObject channel,
                final String events, final String checkpoint) throws Exception {
            if (socket) {
                this.request(channel, events, checkpoint);
                return this.connection.take("response").get("data").objectValueOrThrow();
            }
            return ProtocolTestBrowser.json(this.browser.post(Map.of("action", "synchronize",
                "client", channel.get("id").stringValue(), "events", events,
                "lastUpdate", checkpoint), this.browser.headers(channel)));
        }

        /**
         * @param channel logical page
         * @param events unchanged event batch
         * @param checkpoint unchanged replay position
         */
        void request(final JsonObject channel, final String events, final String checkpoint)
                throws Exception {
            this.push.onText(this.connection, "{\"type\":\"ackHint\",\"client\":\""
                + channel.get("id").stringValue() + "\"}");
            final JsonObject query = new JsonObject();
            query.addString("action", "synchronize");
            query.addString("client", channel.get("id").stringValue());
            query.addString("lastUpdate", checkpoint);
            query.addElement("events", Json.parse(events, JsonParsingMode.STRICT));
            final JsonObject packet = new JsonObject();
            packet.addString("type", "request");
            packet.addNumber("id", ++this.requestId);
            packet.addElement("query", query);
            packet.addElement("channel", this.credentials(channel));
            this.push.onText(this.connection, packet.toString());
        }

        @Override
        public void close() {
            this.app.close();
        }
    }

    /**
     * Authenticated handler boundary: stores envelopes without replacing native transport tests.
     */
    private static final class Connection implements StreamConnection {
        /**
         * Owner established by real HTTP bootstrap.
         */
        private final BrowserSession owner;
        /**
         * Captured application frames.
         */
        private final LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();
        /**
         * Physical close fence.
         */
        private volatile boolean open = true;

        /**
         * @param owner authenticated browser
         */
        Connection(final BrowserSession owner) {
            this.owner = owner;
        }

        @Override
        public BrowserSession owner() {
            return this.owner;
        }

        @Override
        public boolean isOpen() {
            return this.open && this.owner.isActive();
        }

        @Override
        public boolean send(final String text) {
            if (!this.isOpen()) {
                return false;
            }
            this.messages.add(text);
            return true;
        }

        @Override
        public void close() {
            this.open = false;
        }

        /**
         * @param type expected envelope, ignoring coalesced invalidation hints
         * @return matching frame within the bounded test deadline
         */
        JsonObject take(final String type) throws Exception {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (System.nanoTime() < deadline) {
                final String text = this.messages.poll(100, TimeUnit.MILLISECONDS);
                if (text != null) {
                    final JsonObject frame = Json.parse(text, JsonParsingMode.STRICT)
                        .objectValueOrThrow();
                    if (type.equals(frame.get("type").stringValue())) {
                        return frame;
                    }
                }
            }
            assertNotNull("Missing frame: " + type, null);
            throw new AssertionError(type);
        }
    }
}
