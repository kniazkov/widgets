/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonNull;
import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A widget, that is, a user interface element.
 */
public abstract class Widget {
    /**
     * Widget unique identifier.
     */
    private final UId widgetId;

    /**
     * Set of updates to be sent to a client.
     */
    private final List<Instruction> updates;

    /**
     * Constructor.
     */
    public Widget() {
        this.widgetId = UId.create();
        this.updates = new ArrayList<>();
    }

    /**
     * Returns the unique identifier of the widget.
     * @return Identifier
     */
    @NotNull UId getWidgetId() {
        return this.widgetId;
    }

    /**
     * Handles event that was sent by a client.
     * @param json JSON object containing event type and data
     * @param type Event type extracted from JSON object
     */
    abstract void handleEvent(final @NotNull JsonObject json, final @NotNull String type);

    /**
     * Handles event that was sent by a client.
     * @param json JSON object containing event type and data
     */
    void handleEvent(final @NotNull JsonObject json) {
        this.handleEvent(
            json,
            json.getOrDefault("type", JsonNull.INSTANCE).getStringValue()
        );
    }

    /**
     * Adds an instruction (update) to the list of instructions to be sent to a client.
     * @param instruction Instruction
     */
    void sendToClient(final @NotNull Instruction instruction) {
        this.updates.add(instruction);
    }
}
