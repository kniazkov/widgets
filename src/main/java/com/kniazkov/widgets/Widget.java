/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonNull;
import com.kniazkov.json.JsonObject;

/**
 * A widget, that is, a user interface element.
 */
public abstract class Widget {
    /**
     * Widget unique identifier.
     */
    private final UId widgetId;

    /**
     * Constructor.
     */
    public Widget() {
        this.widgetId = UId.create();
    }

    /**
     * Returns the unique identifier of the widget.
     * @return Identifier
     */
    UId getWidgetId() {
        return this.widgetId;
    }

    /**
     * Handles event that was sent by a client.
     * @param json JSON object containing event type and data
     * @param type Event type extracted from JSON object
     */
    abstract void handleEvent(final JsonObject json, final String type);

    /**
     * Handles event that was sent by a client.
     * @param json JSON object containing event type and data
     */
    void handleEvent(final JsonObject json) {
        this.handleEvent(
            json,
            json.getOrDefault("type", JsonNull.INSTANCE).getStringValue()
        );
    }
}
