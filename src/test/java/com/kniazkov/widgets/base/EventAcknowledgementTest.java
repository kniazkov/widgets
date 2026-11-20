/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.EventTestData;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Section;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

/**
 * Delivery acknowledgements must make progress without replaying application side effects.
 */
public final class EventAcknowledgementTest {
    /**
     * A click captured before removal is terminal even when it is the only queued event.
     */
    @Test
    public void acknowledgesRemovedTarget() {
        final Client client = new Client();
        final Button button = new Button("Remove me");
        final AtomicInteger calls = new AtomicInteger();
        button.onClick(event -> calls.incrementAndGet());
        client.getRootWidget().add(new Section(button));
        final JsonObject event = click(button, 1);
        button.remove();
        final JsonObject response = send(client, event);
        assertEquals("#1", response.get("lastEvent").stringValue());
        assertResult(response, 0, "obsolete", "detachedWidget");
        assertResult(send(client, event), 0, "obsolete", "alreadyConsumed");
        assertEquals(0, calls.get());
    }

    /**
     * A mixed batch advances past rejections and removed targets to the next valid action.
     */
    @Test
    public void consumesMixedBatchAndDuplicates() {
        final Client client = new Client();
        final Button remove = new Button("Remove");
        final Button stale = new Button("Stale");
        final Button valid = new Button("Valid");
        final Button disabled = new Button("Disabled");
        final AtomicInteger calls = new AtomicInteger();
        remove.onClick(event -> stale.remove());
        stale.onClick(event -> calls.addAndGet(100));
        disabled.onClick(event -> calls.addAndGet(1000));
        disabled.disable();
        valid.onClick(event -> calls.incrementAndGet());
        client.getRootWidget().add(new Section(remove, stale, disabled, valid));
        final JsonObject[] batch = {click(remove, 1), click(stale, 2), click(disabled, 3),
            click(valid, 4), click(valid, 4), click(valid, 5)};
        final JsonObject response = send(client, batch);
        assertResult(response, 0, "applied", "");
        assertResult(response, 1, "obsolete", "detachedWidget");
        assertResult(response, 2, "rejected", "inactiveWidget");
        assertResult(response, 3, "applied", "");
        assertResult(response, 4, "obsolete", "alreadyConsumed");
        assertResult(response, 5, "applied", "");
        assertEquals("#5", response.get("lastEvent").stringValue());
        assertEquals(2, calls.get());
        final JsonObject replay = send(client, batch);
        for (int index = 0; index < batch.length; index++) {
            assertResult(replay, index, "obsolete", "alreadyConsumed");
        }
        assertEquals(2, calls.get());
        send(client, click(valid, 6));
        assertEquals(3, calls.get());
    }

    /**
     * A malformed envelope with a trustworthy ID is rejected and acknowledged.
     */
    @Test
    public void rejectsMalformedEnvelopeWithoutBlockingFollowingInput() {
        final Client client = new Client();
        final Button button = new Button("Run");
        final AtomicInteger calls = new AtomicInteger();
        button.onClick(event -> calls.incrementAndGet());
        client.getRootWidget().add(new Section(button));
        int id = 0;
        for (final String key : List.of("widget", "type")) {
            final JsonObject invalid = click(button, ++id);
            invalid.addNumber(key, 42);
            final JsonObject response = send(client, invalid);
            assertResult(response, 0, "rejected", "invalidEnvelope");
            assertEquals("#" + id, response.get("lastEvent").stringValue());
        }
        send(client, click(button, ++id));
        assertEquals(1, calls.get());
    }

    /**
     * Invalid or descending IDs cannot skip an earlier action through a cumulative ACK.
     */
    @Test
    public void requestsResynchronizationForUntrustworthySequence() {
        final Client client = new Client();
        final Button button = new Button("Run");
        final AtomicInteger calls = new AtomicInteger();
        button.onClick(event -> calls.incrementAndGet());
        client.getRootWidget().add(new Section(button));
        for (final String id : List.of("#?", "#0", "#-1", "#01", "#+1", "nonsense")) {
            final JsonObject invalid = click(button, 2);
            invalid.addString("id", id);
            final JsonObject response = send(client, click(button, 1), invalid);
            assertTrue(response.get("eventResyncRequired").booleanValue());
            assertEquals("#?", response.get("lastEvent").stringValue());
        }
        final JsonObject response = send(client, click(button, 2), click(button, 1));
        assertTrue(response.get("eventResyncRequired").booleanValue());
        assertEquals(0, calls.get());
        assertEquals("#?", response.get("lastEvent").stringValue());
    }

    /**
     * Delivery is consumed before application code that may throw after a side effect.
     */
    @Test
    public void doesNotRepeatThrowingController() {
        final Client client = new Client();
        final Button button = new Button("Run");
        final AtomicInteger calls = new AtomicInteger();
        button.onClick(event -> {
            calls.incrementAndGet();
            throw new IllegalStateException("Application failure after side effect");
        });
        client.getRootWidget().add(new Section(button));
        final JsonObject event = click(button, 1);
        assertThrows(IllegalStateException.class, () -> send(client, event));
        final JsonObject retry = send(client, event);
        assertEquals(1, calls.get());
        assertEquals("#1", retry.get("lastEvent").stringValue());
        assertResult(retry, 0, "obsolete", "alreadyConsumed");
    }

    /**
     * Losing an HTTP response and retrying the whole batch never repeats either real click.
     */
    @Test
    public void acknowledgesLostResponseAndDoubleClickOverHttp() throws Exception {
        final Button button = new Button("Run");
        final AtomicInteger calls = new AtomicInteger();
        button.onClick(event -> calls.incrementAndGet());
        final Application app = new Application((root, context) ->
            root.add(new Section(button)));
        final var server = Server.start(app, new Options.Builder().setPort(0).build());
        try {
            final var browser = new ProtocolTestBrowser(server.getPort());
            final JsonObject channel = browser.create(null);
            final JsonArray batch = new JsonArray();
            batch.add(click(button, 1));
            batch.add(click(button, 2));
            final Map<String, String> request = Map.of("action", "synchronize", "client",
                channel.get("id").stringValue(), "events", batch.toString());
            assertEquals(200, browser.post(request, browser.headers(channel)).statusCode());
            final var retried = browser.post(request, browser.headers(channel));
            assertEquals(200, retried.statusCode());
            final JsonObject response = ProtocolTestBrowser.json(retried);
            assertEquals("#2", response.get("lastEvent").stringValue());
            assertResult(response, 0, "obsolete", "alreadyConsumed");
            assertResult(response, 1, "obsolete", "alreadyConsumed");
            assertEquals(2, calls.get());
        } finally {
            server.stop();
        }
    }

    /**
     * Creates a structurally valid pointer event with a browser-local sequence number.
     */
    private static JsonObject click(final Button button, final int id) {
        final JsonObject event = new JsonObject();
        event.addString("id", "#" + id);
        event.addString("widget", button.getId().toString());
        event.addString("type", "click");
        event.put("data", EventTestData.pointer());
        return event;
    }

    /**
     * Sends a batch through the actual dispatcher and returns its delivery response.
     */
    private static JsonObject send(final Client client, final JsonObject... events) {
        final JsonArray batch = new JsonArray();
        for (final JsonObject event : events) {
            batch.add(event);
        }
        final JsonObject response = new JsonObject();
        client.synchronize(Map.of("events", batch.toString()), response);
        return response;
    }

    /**
     * Asserts a terminal status and its optional stable machine-readable reason.
     */
    private static void assertResult(final JsonObject response, final int index,
            final String status, final String reason) {
        final JsonObject result = response.get("eventResults").arrayValueOrThrow()
            .get(index).objectValueOrThrow();
        assertEquals(status, result.get("status").stringValue());
        assertEquals(reason, result.containsKey("reason")
            ? result.get("reason").stringValue() : "");
    }
}
