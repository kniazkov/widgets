/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonNull;
import com.kniazkov.json.JsonObject;
import java.util.List;
import java.util.Map;

/**
 * Action handler that synchronizes the state between client and server.
 * <p>
 *     This handler receives the client ID, collects all pending {@link Instruction}s for
 *     that client, serializes them into a JSON response, and sends them back to the client
 *     for execution.
 * </p>
 */
final class Synchronize extends ActionHandler {
    /**
     * Constructs a new synchronization handler.
     *
     * @param application The application instance
     * @param logger The logger to use
     */
    Synchronize(final Application application, final Logger logger) {
        super(application, logger);
    }

    @Override
    JsonElement process(final Map<String, String> data) {
        // Extract client ID from request
        if (!data.containsKey("client")) {
            return JsonNull.INSTANCE;
        }

        final UId clientId = UId.parse(data.get("client"));

        // Create response object
        final JsonObject obj = new JsonObject();
        final JsonArray updates = obj.createArray("updates");

        // Collect and serialize all pending instructions for this client
        final List<Instruction> instructions = application.getUpdates(clientId);
        for (final Instruction instruction : instructions) {
            instruction.serialize(updates.createObject());
        }

        return obj;
    }
}
