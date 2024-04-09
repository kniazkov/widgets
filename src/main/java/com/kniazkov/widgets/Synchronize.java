/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonNull;
import com.kniazkov.json.JsonObject;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Action handler that synchronizes the client and server states
 */
final class Synchronize extends ActionHandler {
    /**
     * Constructor.
     * @param application Application
     * @param logger Logger
     */
    Synchronize(final @NotNull Application application, final @NotNull Logger logger) {
        super(application, logger);
    }

    @Override
    @NotNull JsonElement process(final @NotNull Map<String, String> data) {
        final UId clientId;
        if (data.containsKey("client")) {
            clientId = UId.parse(data.get("client"));
        } else {
            return JsonNull.INSTANCE;
        }
        final JsonObject obj = new JsonObject();
        final JsonArray updates = obj.createArray("updates");
        final List<Instruction> instructions = application.getUpdates(clientId);
        for (final Instruction instruction : instructions) {
            instruction.serialize(updates.createObject());
        }
        return obj;
    }
}
