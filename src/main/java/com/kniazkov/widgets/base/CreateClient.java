/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Action handler that creates a new client instance.
 * This handler is triggered when the client requests a new page session.
 * It creates a new {@link Client}, logs the creation, and returns its unique ID.
 */
final class CreateClient extends ActionHandler {
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

    /**
     * Constructs a new instance handler.
     *
     * @param application the application instance
     */
    CreateClient(final Application application) {
        super(application);
    }

    @Override
    JsonElement process(final Map<String, String> data) {
        // Prepare a collection of parameters that are passed through the address line
        final Map<String, String> parameters = new TreeMap<>(data);
        parameters.remove("action");
        parameters.remove("address");

        // Create a new client and obtain its ID
        String id = this.application.createClient(
            data.get("address"),
            Collections.unmodifiableMap(parameters)
        ).toString();

        // Build a response JSON object with the new client ID
        JsonObject obj = new JsonObject();
        obj.addString("id", id);

        // Log creation for debugging or monitoring
        LOGGER.info("Client " + id + " has been created.");

        return obj;
    }
}
