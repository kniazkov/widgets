/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.*;

import java.util.Map;
import java.util.Optional;

/**
 * Action handler that processes an event sent from the client.
 * <p>
 *     The handler receives the client ID, widget ID, event type, and optional payload,
 *     and passes them to the application for dispatch. If any required field is missing,
 *     the event is ignored and {@code false} is returned.
 * </p>
 */
final class ProcessEvent extends ActionHandler {
    /**
     * Constructs a new event handler.
     *
     * @param application The application instance
     * @param logger The logger to use
     */
    ProcessEvent(final Application application, final Logger logger) {
        super(application, logger);
    }

    @Override
    JsonElement process(final Map<String, String> data) {
        // Check required fields
        if (!data.containsKey("client") || !data.containsKey("widget")
                || !data.containsKey("type")) {
            return JsonBoolean.getInstance(false);
        }

        // Parse client and widget IDs
        final UId clientId = UId.parse(data.get("client"));
        final UId widgetId = UId.parse(data.get("widget"));
        final String eventType = data.get("type");

        // Optional JSON payload
        Optional<JsonObject> payload = Optional.empty();
        if (data.containsKey("data")) {
            try {
                payload = Optional.ofNullable(Json.parse(data.get("data")).toJsonObject());
            } catch (JsonException e) {
                logger.write("Incorrect data format for 'process event' action.");
            }
        }

        // Dispatch event to application
        application.handleEvent(clientId, widgetId, eventType, payload);
        return JsonBoolean.getInstance(true);
    }
}
