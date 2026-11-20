/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;
import com.kniazkov.widgets.protocol.CreateWidget;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Exercises the server-side publication and HTTP negotiation contract.
 */
public class UpdateStreamTest {
    /**
     * Global record IDs can have gaps; replay preserves contiguous channel sequence numbers.
     */
    @Test
    public void publishesAndReplaysChannelSequence() {
        final UpdateStream stream = new UpdateStream();
        final CreateWidget first = new CreateWidget(RMId.create(), "root");
        RMId.create();
        RMId.create();
        final CreateWidget second = new CreateWidget(RMId.create(), "section");
        stream.publish(List.of(first, second));

        final JsonObject initial = response(stream);
        assertTrue(initial.get("snapshot").booleanValue());
        assertEquals("0", initial.get("updateBase").toString());
        assertEquals("2", initial.get("updateThrough").toString());
        final JsonArray batch = initial.get("updates").arrayValueOrThrow();
        assertEquals("1", batch.get(0).objectValueOrThrow().get("sequence").toString());
        assertEquals("2", batch.get(1).objectValueOrThrow().get("sequence").toString());
        assertEquals(first.getId().toString(),
            batch.get(0).objectValueOrThrow().get("id").stringValue());
        assertEquals(second.getId().toString(),
            batch.get(1).objectValueOrThrow().get("id").stringValue());

        assertFalse(stream.acknowledge("former-epoch", "#1", true));
        assertFalse(stream.acknowledge(stream.epoch(), "#3", true));
        assertFalse(stream.acknowledge(stream.epoch(), "#01", true));
        assertEquals(initial.toString(), response(stream).toString());
        assertTrue(stream.acknowledge(stream.epoch(), "#1", true));
        assertTrue(stream.acknowledge(stream.epoch(), "#0", true));
        final JsonObject suffix = response(stream);
        assertFalse(suffix.get("snapshot").booleanValue());
        assertEquals("1", suffix.get("updateBase").toString());
        assertEquals(1, suffix.get("updates").arrayValueOrThrow().size());
        assertEquals(batch.get(1).toString(), suffix.get("updates").arrayValueOrThrow()
            .get(0).toString());
    }

    /**
     * Version/capability negotiation and stale ACK checking occur on real authenticated HTTP.
     */
    @Test
    public void negotiatesFreshSnapshotsAndRejectsIncompatibleClients() throws Exception {
        final var app = new Application((root, context) -> { });
        final var server = Server.start(app, new Options.Builder().setPort(0).build());
        try {
            final var browser = new ProtocolTestBrowser(server.getPort());
            final var bad = browser.post(Map.of("action", "new instance", "address", "/",
                "browserId", ProtocolTestBrowser.BROWSER_ID, "mobile", "false", "updateVersion",
                "1", "updateCapability", UpdateStream.CAPABILITY), browser.headers(null));
            assertTrue(ProtocolTestBrowser.json(bad).get("protocolMismatch").booleanValue());

            final JsonObject created = browser.create(null);
            assertEquals(UpdateStream.VERSION, created.get("updateVersion").stringValue());
            assertEquals(UpdateStream.CAPABILITY, created.get("updateCapability").stringValue());
            final var request = Map.of("action", "synchronize", "lastUpdate", "#0",
                "client", created.get("id").stringValue());
            final JsonObject initial = ProtocolTestBrowser.json(browser.post(request,
                browser.headers(created)));
            assertTrue(initial.get("snapshot").booleanValue());
            assertEquals(created.get("updateEpoch").stringValue(),
                initial.get("updateEpoch").stringValue());

            final JsonObject reconnected = browser.create(created.get("tabId").stringValue());
            assertNotEquals(created.get("updateEpoch").stringValue(),
                reconnected.get("updateEpoch").stringValue());
            final var newRequest = Map.of("action", "synchronize", "lastUpdate", "#0",
                "client", reconnected.get("id").stringValue());
            final var stale = browser.headers(reconnected);
            stale.put("X-Widgets-Update-Epoch", created.get("updateEpoch").stringValue());
            final JsonObject refused = ProtocolTestBrowser.json(browser.post(newRequest, stale));
            assertTrue(refused.get("protocolMismatch").booleanValue());
            assertFalse(refused.containsKey("updates"));
            final JsonObject fresh = ProtocolTestBrowser.json(browser.post(newRequest,
                browser.headers(reconnected)));
            assertTrue(fresh.get("snapshot").booleanValue());
            assertEquals("0", fresh.get("updateBase").toString());
        } finally {
            server.stop();
        }
    }

    /**
     * @param stream stream under test
     * @return serialized update frame
     */
    private static JsonObject response(final UpdateStream stream) {
        final JsonObject result = new JsonObject();
        stream.serialize(result);
        return result;
    }
}
