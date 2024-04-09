/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Action handler that creates a new client
 */
final class NewInstance extends ActionHandler {
    /**
     * Constructor.
     * @param application Application
     * @param logger Logger
     */
    NewInstance(final @NotNull Application application, final @NotNull Logger logger) {
        super(application, logger);
    }

    @Override
    @NotNull JsonElement process(final @NotNull Map<String, String> data) {
        final String id = this.application.createClient().toString();
        final JsonObject obj = new JsonObject();
        obj.addString("id", id);
        logger.write("Client " + id + " created.");
        return obj;
    }
}
