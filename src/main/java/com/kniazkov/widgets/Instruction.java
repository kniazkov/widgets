/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;

/**
 * Instruction sent from server to client to describe a UI change.
 * <p>
 *     Each instruction targets a specific widget and defines an action to be performed on the
 *     client side. Instructions are serialized to JSON before being sent, and uniquely identified
 *     using {@link UId}.
 * </p>
 *
 * <p>
 *     Subclasses define specific types of instructions by implementing {@link #getAction()}
 *     and optionally overriding {@link #fillJsonObject(JsonObject)} to include additional data.
 * </p>
 */
abstract class Instruction implements Comparable<Instruction> {
    /**
     * Unique identifier of this instruction (used for sorting or debugging).
     */
    private final UId instrId;

    /**
     * Identifier of the widget that this instruction targets.
     */
    private final UId widgetId;

    /**
     * Creates a new instruction for a specific widget.
     *
     * @param widgetId The ID of the widget affected by this instruction
     */
    Instruction(final UId widgetId) {
        this.instrId = UId.create();
        this.widgetId = widgetId;
    }

    /**
     * Serializes this instruction into a JSON object for client transmission.
     * <p>
     *     This method sets standard fields (ID, widget, action) and then delegates
     *     to {@link #fillJsonObject(JsonObject)} to add any subclass-specific data.
     * </p>
     *
     * @param obj The JSON object to populate
     */
    void serialize(final JsonObject obj) {
        obj.addString("id", this.instrId.toString());
        obj.addString("widget", this.widgetId.toString());
        obj.addString("action", this.getAction());
        this.fillJsonObject(obj);
    }

    /**
     * Returns the action name that defines the type of instruction.
     *
     * @return The action name as a string
     */
    protected abstract String getAction();

    /**
     * Allows subclasses to append additional fields to the instruction JSON.
     * This default implementation does nothing.
     *
     * @param json The JSON object to modify
     */
    protected void fillJsonObject(final JsonObject json) {
        // default: no additional fields
    }

    @Override
    public String toString() {
        final JsonObject obj = new JsonObject();
        this.serialize(obj);
        return obj.toString();
    }

    /**
     * Compares this instruction to another by their unique ID.
     * <p>
     *     Used to ensure consistent ordering of instructions, e.g., in {@code TreeSet} or
     *     {@code TreeMap}.
     * </p>
     *
     * @param other The other instruction to compare with
     * @return Comparison result based on ID ordering
     */
    @Override
    public int compareTo(final Instruction other) {
        return this.instrId.compareTo(other.instrId);
    }
}
