/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the contract of the run-local monotonic identifier.
 */
public class RMIdTest {
    /**
     * Generated identifiers are positive and strictly increase during the current run.
     */
    @Test
    public void generatedIdentifiersIncreaseWithinTheRun() {
        final RMId first = RMId.create();
        final RMId second = RMId.create();

        assertTrue(first.isValid());
        assertTrue(second.compareTo(first) > 0);
    }

    /**
     * The compact wire representation remains round-trippable.
     */
    @Test
    public void serializationRoundTrips() {
        final RMId id = RMId.create();

        assertEquals(id, RMId.parse(id.toString()));
    }
}
