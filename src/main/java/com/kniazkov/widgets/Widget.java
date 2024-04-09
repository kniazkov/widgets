/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonNull;
import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        this.updates.add(new Create(this.widgetId, this.getType()));
    }

    /**
     * Receives a visitor, that is a class that performs some operation with widgets.
     * @param visitor A visitor
     * @return Result of operation
     */
    public abstract boolean accept(final @NotNull WidgetVisitor visitor);

    /**
     * Returns the unique identifier of the widget.
     * @return Identifier
     */
    @NotNull UId getWidgetId() {
        return this.widgetId;
    }

    /**
     * Returns type of the widget.
     * @return Type of widget represented as a string
     */
    abstract @NotNull String getType();

    /**
     * Handles event that was sent by a client.
     * @param type Event type
     * @param data Event-related data
     */
    abstract void handleEvent(final @NotNull String type, final @Nullable JsonObject data);

    /**
     * Adds an instruction (update) to the list of instructions to be sent to a client.
     * @param instruction Instruction
     */
    void sendToClient(final @NotNull Instruction instruction) {
        this.updates.add(instruction);
    }

    /**
     * Takes the list of updates and clears it from the widget itself
     * @param allUpdates A list containing all updates from all widgets
     */
    void getUpdates(final @NotNull List<Instruction> allUpdates) {
        allUpdates.addAll(this.updates);
        this.updates.clear();
    }
}
