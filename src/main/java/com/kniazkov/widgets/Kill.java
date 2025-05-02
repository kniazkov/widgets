/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonBoolean;
import com.kniazkov.json.JsonElement;
import java.util.Map;

/**
 * Action handler that terminates a client session.
 * <p>
 *     This handler is invoked automatically by the client when the user closes the browser tab
 *     or reloads the page. It allows the server to clean up associated resources and remove
 *     the client from memory.
 * </p>
 */
final class Kill extends ActionHandler {
    /**
     * Constructs a new kill handler.
     *
     * @param application The application instance
     * @param logger The logger to use
     */
    Kill(final Application application, final Logger logger) {
        super(application, logger);
    }

    @Override
    JsonElement process(final Map<String, String> data) {
        boolean result = false;

        if (data.containsKey("client")) {
            UId clientId = UId.parse(data.get("client"));
            result = application.killClient(clientId);

            if (result) {
                logger.write("Client " + clientId + " is killed.");
            }
        }

        return JsonBoolean.getInstance(result);
    }
}
