/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

/**
 * Instructions for creating widgets.
 */
final class Create extends Instruction {
    /**
     * Type of widget to create.
     */
    private final String type;

    /**
     * Constructor.
     *
     * @param widgetId Identifier of the widget the instruction is working with
     * @param type Type of widget to create
     */
    Create(final @NotNull UId widgetId, final @NotNull String type) {
        super(widgetId);
        this.type = type;
    }

    @Override
    protected @NotNull String getAction() {
        return "create";
    }

    @Override
    protected void fillJsonObject(final @NotNull JsonObject json) {
        json.addString("type", this.type);
    }
}
