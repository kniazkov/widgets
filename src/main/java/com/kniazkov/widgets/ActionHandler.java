/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonElement;

import java.util.Map;

/**
 * Base class for handling actions requested by the client.
 * <p>
 *     Each {@code ActionHandler} processes a specific kind of client-side request,
 *     such as sending events, or handling commands.
 * </p>
 *
 * <p>
 *     The {@link #process(Map)} method must be implemented to handle incoming request data
 *     and return a response in JSON format.
 * </p>
 */
abstract class ActionHandler {
    /**
     * Reference to the application instance.
     */
    protected final Application application;

    /**
     * Logger used for diagnostics and error reporting.
     */
    protected final Logger logger;

    /**
     * Constructs a new action handler.
     *
     * @param application The application instance
     * @param logger The logger for diagnostic output
     */
    ActionHandler(final Application application, final Logger logger) {
        this.application = application;
        this.logger = logger;
    }

    /**
     * Processes a request from the client.
     *
     * @param data Key-value data received from the client
     * @return A JSON element representing the response to send back
     */
    abstract JsonElement process(Map<String, String> data);
}
