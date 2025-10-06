/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonBoolean;
import com.kniazkov.json.JsonElement;
import com.kniazkov.widgets.common.UId;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Action handler that terminates a client session.
 * This handler is invoked automatically by the client when the user closes the browser tab
 * or reloads the page. It allows the server to clean up associated resources and remove
 * the client from memory.
 */
final class KillClient extends ActionHandler {
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

    /**
     * Constructs a new kill handler.
     *
     * @param application the application instance
     */
    KillClient(final Application application) {
        super(application);
    }

    @Override
    JsonElement process(final Map<String, String> data) {
        boolean result = false;

        if (data.containsKey("client")) {
            UId clientId = UId.parse(data.get("client"));
            result = application.killClient(clientId);

            if (result) {
                LOGGER.info("Client " + clientId + " is killed.");
            }
        }

        return JsonBoolean.getInstance(result);
    }
}
