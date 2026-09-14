/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.client.OnClient;
import com.kniazkov.widgets.common.RMId;
import java.util.Objects;

/**
 * An {@link Update} that associates a widget event with an action executed by the browser.
 * The association is stored on the client, so the action runs synchronously in the originating
 * browser event handler without waiting for a server round trip.
 */
public final class SetClientEventAction extends Update {
    /**
     * Event type that triggers the action.
     */
    private final String event;

    /**
     * Action to execute in the browser.
     */
    private final OnClient clientAction;

    /**
     * Creates a new client-side event action update.
     *
     * @param widget the widget identifier
     * @param event the event type that triggers the action
     * @param clientAction the action to execute
     */
    public SetClientEventAction(final RMId widget, final String event,
            final OnClient clientAction) {
        super(widget);
        this.event = Objects.requireNonNull(event, "event");
        this.clientAction = Objects.requireNonNull(clientAction, "clientAction");
    }

    @Override
    public Update clone() {
        return new SetClientEventAction(this.getWidgetId(), this.event, this.clientAction);
    }

    @Override
    protected String getAction() {
        return "set client event action";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("event", this.event);
        json.addElement("clientAction", this.clientAction.toJsonObject());
    }
}
