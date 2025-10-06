/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonElement;

import java.util.Map;

/**
 * Base class for handling actions requested by the client.
 * Each {@code ActionHandler} processes a specific kind of client-side request,
 * such as sending events, or handling commands. The {@link #process(Map)} method
 * must be implemented to handle incoming request data and return a response in JSON format.
 */
abstract class ActionHandler {
    /**
     * Reference to the application instance.
     */
    protected final Application application;

    /**
     * Constructs a new action handler.
     *
     * @param application the application instance
     */
    ActionHandler(final Application application) {
        this.application = application;
    }

    /**
     * Processes a request from the client.
     *
     * @param data Key-value data received from the client
     * @return A JSON element representing the response to send back
     */
    abstract JsonElement process(Map<String, String> data);
}
