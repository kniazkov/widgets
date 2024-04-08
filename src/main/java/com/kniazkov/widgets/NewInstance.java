/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

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
     */
    NewInstance(final @NotNull Application application) {
        super(application);
    }

    @Override
    @NotNull JsonObject process(final @NotNull Map<String, String> data) {
        UId id = this.application.createClient();
        JsonObject obj = new JsonObject();
        obj.addString("id", id.toString());
        return obj;
    }
}
