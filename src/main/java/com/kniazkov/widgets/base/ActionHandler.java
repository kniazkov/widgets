/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for handling actions requested by the client.
 * Each {@code ActionHandler} processes a specific kind of client-side request,
 * such as sending events, or handling commands. The {@link #process(Map)} method
 * must be implemented to handle incoming request data and return a response in JSON format.
 */
abstract class ActionHandler {
    /**
     * Logger for failures at the client protocol boundary.
     */
    private static final Logger LOGGER = Logger.getLogger(
        ActionHandler.class.getName()
    );

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

    /**
     * Processes a request and converts application failures to an explicit
     * client-error response.
     *
     * @param data request data
     * @return protocol response
     */
    final JsonElement processSafely(final Map<String, String> data) {
        try {
            return this.process(data);
        } catch (final RuntimeException | Error failure) {
            LOGGER.log(
                Level.SEVERE,
                "Client request failed in " + this.getClass().getSimpleName(),
                failure
            );
            if (failure instanceof VirtualMachineError fatal) {
                throw fatal;
            }
            return clientError();
        }
    }

    /**
     * Builds the shared fatal client-error response.
     *
     * @return protocol response
     */
    static JsonObject clientError() {
        final JsonObject response = new JsonObject();
        response.addBoolean("result", false);
        response.addBoolean("clientError", true);
        return response;
    }
}
