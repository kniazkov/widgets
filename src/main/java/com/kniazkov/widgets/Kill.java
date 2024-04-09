/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonBoolean;
import com.kniazkov.json.JsonElement;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Action handler that kills a client.
 */
final class Kill extends ActionHandler {
    /**
     * Constructor.
     * @param application Application
     * @param logger Logger
     */
    Kill(final @NotNull Application application, final @NotNull Logger logger) {
        super(application, logger);
    }

    @Override
    @NotNull JsonElement process(final @NotNull Map<String, String> data) {
        boolean result = false;
        if (data.containsKey("client")) {
            final UId clientId = UId.parse(data.get("client"));
            result = application.killClient(clientId);
            if (result) {
                logger.write("Client " + clientId.toString() + " is killed.");
            }
        }
        return JsonBoolean.getInstance(result);
    }
}
