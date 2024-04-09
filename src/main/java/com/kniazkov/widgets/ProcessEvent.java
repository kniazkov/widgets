/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.*;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Action handler that processes an event.
 */
final class ProcessEvent extends ActionHandler {
    /**
     * Constructor.
     * @param application Application
     * @param logger Logger
     */
    ProcessEvent(final @NotNull Application application, final @NotNull Logger logger) {
        super(application, logger);
    }

    @Override
    @NotNull JsonElement process(final @NotNull Map<String, String> data) {
        boolean result = false;
        if (data.containsKey("client") || data.containsKey("widget") || data.containsKey("type")) {
            final UId clientId = UId.parse(data.get("client"));
            final UId widgetId = UId.parse(data.get("widget"));
            JsonObject obj = null;
            if (data.containsKey("data")) {
                try {
                    obj = Json.parse(data.get("data")).toJsonObject();
                } catch (JsonException ignored) {
                    logger.write("Incorrect data format for 'process event' action.");
                }
            }
            application.handleEvent(clientId, widgetId, data.get("type"), obj);
            result = true;
        }
        return JsonBoolean.getInstance(result);
    }
}
