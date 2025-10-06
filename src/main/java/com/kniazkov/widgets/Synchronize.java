/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.base.ActionHandler;
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
        final JsonObject obj = new JsonObject();
        obj.addBoolean("result", false);

        if (data.containsKey("client")) {
            final UId clientId = UId.parse(data.get("client"));
            this.application.synchronize(clientId, data, obj);
        }

        return obj;
    }
}
