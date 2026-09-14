/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.client;

import com.kniazkov.json.JsonObject;

/**
 * A declarative action that is sent to the browser in advance and executed there in response
 * to a client-side interaction.
 *
 * <p>The interface describes the action independently of any particular widget, event, property,
 * or model. Those bindings are intentionally outside this initial contract.</p>
 */
public interface OnClient {
    /**
     * Returns the wire name understood by the browser-side action dispatcher.
     *
     * @return client action name
     */
    String getAction();

    /**
     * Writes action-specific parameters to the supplied JSON object.
     *
     * @param object destination JSON object
     */
    void fillJsonObject(JsonObject object);

    /**
     * Serializes this action for delivery to the browser.
     *
     * @return JSON object containing the action name and its parameters
     */
    default JsonObject toJsonObject() {
        final JsonObject object = new JsonObject();
        object.addString("action", this.getAction());
        this.fillJsonObject(object);
        return object;
    }
}
