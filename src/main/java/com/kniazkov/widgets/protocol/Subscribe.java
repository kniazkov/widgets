/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;

/**
 * An {@link Update} that instructs the client view to subscribe to a specific event
 * on the given widget. To avoid overloading the communication channel, the client does not send
 * every possible event to the server. Instead, it only transmits events that the server
 * has explicitly subscribed to. This {@code Subscribe} update is typically sent at the moment
 * when a controller (event handler) is attached to a widget.
 */
public final class Subscribe extends Update {
    /**
     * The event type that the client should send back to the server
     * when it occurs on the target widget.
     */
    final String event;


    /**
     * Creates a new "subscribe" update.
     *
     * @param widget the widget identifier
     * @param event the event type to subscribe to (must not be {@code null})
     */
    public Subscribe(final UId widget, final String event) {
        super(widget);
        this.event = event;
    }

    @Override
    public Update clone() {
        return new Subscribe(this.getWidgetId(), this.event);
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
