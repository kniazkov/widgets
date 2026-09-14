/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.client;

import com.kniazkov.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

/**
 * Tests the initial client action contract and navigation implementations.
 */
public final class OnClientTest {
    /**
     * Verifies current-tab navigation serialization and value semantics.
     */
    @Test
    public void serializesCurrentTabNavigation() {
        final OnClient action = new OpenPage("/products");
        final JsonObject json = action.toJsonObject();

        assertEquals("go to page", json.get("action").getStringValue());
        assertEquals("/products", json.get("href").getStringValue());
        assertEquals(new OpenPage("/products"), action);
        assertNotEquals(new OpenPage("/orders"), action);
    }

    /**
     * Verifies new-tab navigation serialization and value semantics.
     */
    @Test
    public void serializesNewTabNavigation() {
        final OnClient action = new OpenPageInNewTab("images/original.png");
        final JsonObject json = action.toJsonObject();

        assertEquals("open page in new tab", json.get("action").getStringValue());
        assertEquals("images/original.png", json.get("href").getStringValue());
        assertEquals(new OpenPageInNewTab("images/original.png"), action);
        assertNotEquals(new OpenPage("images/original.png"), action);
    }

    /**
     * Verifies that navigation actions cannot contain an absent URL.
     */
    @Test
    public void rejectsNullHref() {
        assertThrows(NullPointerException.class, () -> new OpenPage(null));
        assertThrows(NullPointerException.class, () -> new OpenPageInNewTab(null));
    }
}
