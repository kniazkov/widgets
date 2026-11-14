/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.RMId;
import com.kniazkov.widgets.protocol.AppendChild;
import com.kniazkov.widgets.protocol.RemoveChild;
import com.kniazkov.widgets.protocol.SetProperty;
import com.kniazkov.widgets.protocol.Update;
import com.kniazkov.widgets.view.Property;
import com.kniazkov.widgets.view.State;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Bounded replay with deterministic clocks and ordinary tiny updates.
 */
public class UpdateStreamBackpressureTest {
    /**
     * Batches never allow a client to acknowledge updates not yet sent.
     */
    @Test
    public void restrictsBatchAndKeepsExactReplay() {
        final var stream = new UpdateStream(
            new ProtocolLimits(8, 4096, 2, 1024, 1000, 8, 1024), System::nanoTime);
        final var id = RMId.create();
        stream.publish(List.of(text(id, "first"), text(id, "second"), text(id, "third")));
        assertFalse(stream.acknowledge(stream.epoch(), "#1", true));
        final var first = batch(stream);
        assertEquals(2, first.get("updates").arrayValueOrThrow().size());
        assertTrue(first.get("moreUpdates").booleanValue());
        assertEquals(first.toString(), batch(stream).toString());
        assertFalse(stream.acknowledge(stream.epoch(), "#3", true));
        assertTrue(stream.acknowledge(stream.epoch(), "#2", true));
        final var last = batch(stream);
        assertFalse(last.get("moreUpdates").booleanValue());
        assertEquals(1, last.get("updates").arrayValueOrThrow().size());
        assertEquals(3, last.get("updateThrough").numberValue().orElseThrow().intValue());
    }

    /**
     * Safe same-property setters combine only before transmission and between barriers.
     */
    @Test
    public void coalescingDoesNotCrossStructuralChanges() {
        final var stream = new UpdateStream();
        final var widget = RMId.create();
        final var parent = RMId.create();
        stream.publish(List.of(
            new AppendChild(widget, parent),
            new SetProperty<>(widget, State.ANY, Property.COLOR, Color.BLACK),
            new SetProperty<>(widget, State.ANY, Property.COLOR, Color.WHITE),
            new RemoveChild(widget, parent),
            new SetProperty<>(widget, State.ANY, Property.COLOR, Color.BLACK),
            text(widget, "first"),
            text(widget, "second")
        ));
        final var replies = batch(stream).get("updates").arrayValueOrThrow();
        assertEquals(6, replies.size());
        assertEquals("append child", replies.get(0).objectValueOrThrow()
            .get("action").stringValue());
        assertEquals("remove child", replies.get(2).objectValueOrThrow()
            .get("action").stringValue());
        assertEquals("first", replies.get(4).objectValueOrThrow().get("text").stringValue());
        assertEquals("second", replies.get(5).objectValueOrThrow().get("text").stringValue());
    }

    /**
     * Capacity and age both abandon the old epoch rather than emitting a partial state.
     */
    @Test
    public void expiresOrRejectsAnUnrecoverableReplayWindow() {
        final var clock = new AtomicLong();
        final var capacity = new ProtocolLimits(2, 4096, 2, 256, 10, 8, 1024);
        final var stream = new UpdateStream(capacity, clock::get);
        final var widget = RMId.create();
        stream.publish(List.of(text(widget, "one")));
        batch(stream);
        clock.set(TimeUnit.MILLISECONDS.toNanos(10));
        final var expired = batch(stream);
        assertTrue(expired.get("updateResyncRequired").booleanValue());
        assertFalse(expired.get("snapshotUnavailable").booleanValue());
        assertFalse(expired.containsKey("updates"));

        final var oversized = new UpdateStream(capacity, clock::get);
        oversized.publish(List.of(text(widget, "x".repeat(256))));
        assertTrue(batch(oversized).get("snapshotUnavailable").booleanValue());
    }

    /**
     * @param widget target
     * @param text ordinary text
     * @return update
     */
    private static Update text(final RMId widget, final String text) {
        return new SetProperty<>(widget, State.ANY, Property.TEXT, text);
    }

    /**
     * @param stream exercised stream
     * @return wire response
     */
    private static JsonObject batch(final UpdateStream stream) {
        final var response = new JsonObject();
        stream.serialize(response);
        return response;
    }
}
