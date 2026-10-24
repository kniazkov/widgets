/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.RMId;
import com.kniazkov.widgets.protocol.SetCountdownDeadline;
import java.time.Instant;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

/**
 * Deadline validation and browser protocol replacement semantics.
 */
public final class CountdownTest {
    /**
     * Preserves absolute milliseconds through initial creation and replacement.
     */
    @Test
    public void sendsAbsoluteDeadline() {
        final Countdown countdown = new Countdown(Instant.ofEpochMilli(123456));
        final var sandbox = WidgetSandbox.open(countdown);
        final var first = sandbox.drainUpdates().stream()
            .filter(json -> json.get("action").stringValue().equals("set countdown deadline"))
            .findFirst().orElseThrow();
        assertEquals(123456L, first.get("deadline").longValue());
        countdown.setDeadline(Instant.ofEpochMilli(-100));
        assertEquals(Instant.ofEpochMilli(-100), countdown.getDeadline());
        final var updates = sandbox.drainUpdates();
        assertEquals(1, updates.size());
        assertEquals(-100L, updates.get(0).get("deadline").longValue());
    }

    /**
     * Rejects null and unrepresentable dates without modifying the existing deadline.
     */
    @Test
    public void validatesDates() {
        final Countdown countdown = new Countdown(Instant.EPOCH);
        assertThrows(NullPointerException.class, () -> countdown.setDeadline(null));
        assertThrows(IllegalArgumentException.class, () -> countdown.setDeadline(Instant.MAX));
        assertThrows(IllegalArgumentException.class,
            () -> countdown.setDeadline(Instant.ofEpochMilli(8640000000000001L)));
        assertEquals(Instant.EPOCH, countdown.getDeadline());
        countdown.setDeadline(Instant.ofEpochSecond(1, 123456789));
        assertEquals(Instant.ofEpochMilli(1123), countdown.getDeadline());
    }

    /**
     * Reattachment clones the protocol checkpoint without changing its deadline.
     */
    @Test
    public void clonesDeadline() {
        final var update = new SetCountdownDeadline(RMId.create(), 1234);
        final var copy = update.clone();
        assertNotEquals(update.getId(), copy.getId());
        assertEquals(update.getWidgetId(), copy.getWidgetId());
        final var json = new com.kniazkov.json.JsonObject();
        copy.serialize(json);
        assertEquals(1234, json.get("deadline").longValue());
    }
}
