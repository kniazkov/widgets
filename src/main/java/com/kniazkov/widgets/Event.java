/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

/**
 * An event that was submitted by a client.
 */
interface Event {
    /**
     * Returns widget identifier as UId object.
     * @return Widget identifier
     */
    @NotNull UId getWidgetId();

    /**
     * Applies the event to a widget.
     * @param widget Widget
     */
    void apply(@NotNull Widget widget);

    /**
     * Parses Parses an event from a JSON object that describes this event
     * @param json JSON object
     * @return An event or {@code null} if event cannot be parsed
     */
    static Event parse(final @NotNull JsonObject json) {
        if (json.containsKey("type") && json.containsKey("widgetId")) {
            JsonElement type = json.getElement("type");
            if (type.isString()) {
                switch (type.getStringValue()) {
                    case "click":
                        return json.toJavaObject(ClickEvent.class);
                }
            }
        }
        return null;
    }
}
