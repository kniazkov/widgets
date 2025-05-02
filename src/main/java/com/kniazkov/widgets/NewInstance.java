/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;

import java.util.Map;

/**
 * Action handler that creates a new client instance.
 * <p>
 *     This handler is triggered when the client requests a new page session.
 *     It creates a new {@link Client}, logs the creation, and returns its unique ID.
 * </p>
 */
final class NewInstance extends ActionHandler {
    /**
     * Constructs a new instance handler.
     *
     * @param application The application instance
     * @param logger The logger to use
     */
    NewInstance(final Application application, final Logger logger) {
        super(application, logger);
    }

    @Override
    JsonElement process(final Map<String, String> data) {
        // Create a new client and obtain its ID
        String id = this.application.createClient().toString();

        // Build a response JSON object with the new client ID
        JsonObject obj = new JsonObject();
        obj.addString("id", id);

        // Log creation for debugging or monitoring
        logger.write("Client " + id + " has been created.");

        return obj;
    }
}
