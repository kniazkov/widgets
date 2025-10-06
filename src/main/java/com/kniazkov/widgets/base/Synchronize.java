/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;
import com.kniazkov.widgets.protocol.Update;
import java.util.Map;

/**
 * Action handler that synchronizes the state between client and server.
 * This handler receives the client ID, collects all pending {@link Update}s for
 * that client, serializes them into a JSON response, and sends them back to the client
 * for execution.
 */
final class Synchronize extends ActionHandler {
    /**
     * Constructs a new synchronization handler.
     *
     * @param application the application instance
     */
    Synchronize(final Application application) {
        super(application);
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
