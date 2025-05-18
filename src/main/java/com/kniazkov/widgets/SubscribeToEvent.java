/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;

/**
 * An instruction sent to the client to subscribe to a specific event type for a given widget.
 * <p>
 *     By default, the client processes events locally and does not transmit them to the server.
 *     This instruction explicitly requests the client to begin sending events of the specified
 *     type, such as {@code "click"} or {@code "pointer enter"}, for the identified widget.
 * </p>
 *
 * <p>
 *     This mechanism exists to avoid unnecessary network load, since the client may generate
 *     a large volume of events. Typically, only a small subset (e.g., {@code "click"}) is relevant
 *     to the server, such as when handling interactions with buttons or interactive controls.
 * </p>
 */
final class SubscribeToEvent extends Instruction {

    /**
     * The event type to subscribe to (e.g. "click", "pointer enter").
     */
    final String event;

    /**
     * Creates a new subscription instruction for a specific widget and event type.
     *
     * @param widgetId The ID of the widget this instruction applies to
     * @param event The name of the event to subscribe to
     */
    SubscribeToEvent(final UId widgetId, final String event) {
        super(widgetId);
        this.event = event;
    }

    @Override
    protected String getAction() {
        return "subscribe";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("event", this.event);
    }
}
