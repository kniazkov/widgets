/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

/**
 * Instruction that is sent from server to client to display changes.
 */
abstract class Instruction {
    /**
     * Instruction unique identifier.
     */
    private final UId instrId;

    /**
     * Identifier of the widget the instruction is working with.
     */
    private final UId widgetId;

    /**
     * Constructor.
     * @param widgetId Identifier of the widget the instruction is working with
     */
    Instruction(final @NotNull UId widgetId) {
        this.instrId = UId.create();
        this.widgetId = widgetId;
    }

    /**
     * Serialization of an instruction, i.e., transforming it into a JSON object
     *  for further transmission to a client.
     * @param obj Resulting JSON object
     */
    void serialize(final JsonObject obj) {
        obj.addString("instrId", this.instrId.toString());
        obj.addString("widgetId", this.widgetId.toString());
        obj.addString("action", this.getAction());
        this.fillJsonObject(obj);
    }

    /**
     * Returns an action, that is, a description of what the instruction should do.
     * @return An action as a string
     */
    protected abstract @NotNull String getAction();

    /**
     * Fills a JSON object with the data needed to execute the instruction on the client side.
     * @param json JSON object to be filled
     */
    protected void fillJsonObject(final @NotNull JsonObject json) {
        // do nothing; this method is expected to be overridden
    }

    @Override
    public String toString() {
        final JsonObject obj = new JsonObject();
        this.serialize(obj);
        return obj.toString();
    }
}
