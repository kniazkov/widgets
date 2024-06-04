/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.TreeSet;
import java.util.Set;

/**
 * A widget, that is, a user interface element.
 */
public abstract class Widget {
    /**
     * Widget unique identifier.
     */
    private final UId widgetId;

    /**
     * Parent (owner) of this widget.
     */
    private Widget parent;

    /**
     * Client (owner of hierarchical tree of widgets which includes this widget).
     * The reference of it appears as soon as the widget is added to hierarchical tree of widgets.
     */
    final Future<Client> client;

    /**
     * Set of updates to be sent to a client.
     */
    private final Set<Instruction> updates;

    /**
     * Constructor.
     */
    public Widget() {
        this.widgetId = UId.create();
        this.parent = null;
        this.client = new Future<>();
        this.updates = new TreeSet<>();
        this.updates.add(new Create(this.widgetId, this.getType()));
    }

    /**
     * Sets the parent. This can be done only once.
     * @param parent Parent widget
     */
    void setParent(final @NotNull Widget parent) {
        assert this.parent == null;
        this.parent = parent;
        parent.client.addListener(client -> {
            Widget.this.client.setData(client);
            client.widgets.put(Widget.this.getWidgetId(), Widget.this);
        });
    }

    /**
     * Returns the parent, that is, the owner of this widget.
     * @return Parent widget
     */
    public Widget getParent() {
        return this.parent;
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
     * @param allUpdates A set containing all updates from all widgets
     */
    void getUpdates(final @NotNull Set<Instruction> allUpdates) {
        allUpdates.addAll(this.updates);
        this.updates.clear();
    }
}
