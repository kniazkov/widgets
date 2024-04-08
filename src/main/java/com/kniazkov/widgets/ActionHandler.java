/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Handler for actions that are requested by clients.
 */
abstract class ActionHandler {
    /**
     * Application.
     */
    protected final Application application;

    /**
     * Logger.
     */
    protected final Logger logger;

    /**
     * Constructor.
     * @param application Application
     * @param logger Logger
     */
    ActionHandler(final @NotNull Application application, final @NotNull Logger logger) {
        this.application = application;
        this.logger = logger;
    }

    /**
     * Processes the action requested by the client.
     * @param data Data transmitted by the client
     * @return Result of request processing in JSON format
     */
    abstract @NotNull JsonObject process(final @NotNull Map<String, String> data);
}
