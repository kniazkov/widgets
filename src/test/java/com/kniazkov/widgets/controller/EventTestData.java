/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

import com.kniazkov.json.JsonObject;

/**
 * Complete browser pointer payloads shared by protocol regression tests.
 */
public final class EventTestData {
    /**
     * Utility class.
     */
    private EventTestData() {
    }

    /**
     * Creates a valid mouse click payload.
     * @return mutable test payload
     */
    public static JsonObject pointer() {
        final JsonObject data = new JsonObject();
        data.addString("type", "mouse");
        data.addBoolean("primary", true);
        data.addNumber("buttons", 0);
        data.addNumber("pressure", 0);
        final JsonObject keys = data.createObject("keys");
        for (final String name : new String[]{"ctrl", "alt", "shift", "meta"}) {
            keys.addBoolean(name, false);
        }
        final JsonObject position = data.createObject("position");
        for (final String name : new String[]{"element", "client", "page", "screen"}) {
            final JsonObject point = position.createObject(name);
            point.addNumber("x", 10);
            point.addNumber("y", 20);
        }
        return data;
    }
}
