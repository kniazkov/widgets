/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;

/**
 * Instruction for creating a widget on the client side.
 * <p>
 *     This instruction tells the client to instantiate a new widget of a specified type,
 *     associated with a given widget ID.
 * </p>
 */
final class Create extends Instruction {
    /**
     * Type of widget to create (e.g., "text", "button", "input field").
     */
    private final String type;

    /**
     * Constructs a new creation instruction for a widget of the given type.
     *
     * @param widgetId The identifier of the widget to be created
     * @param type The type of widget to create
     */
    Create(final UId widgetId, final String type) {
        super(widgetId);
        this.type = type;
    }

    @Override
    protected String getAction() {
        return "create";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("type", this.type);
    }
}