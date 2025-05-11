/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import java.util.Optional;
import java.util.TreeSet;
import java.util.Set;

/**
 * Abstract base class representing a user interface element (widget).
 * <p>
 *     A {@code Widget} is a server-side representation of a UI component that may appear
 *     in the client’s browser. It is responsible for managing its identity, hierarchical ownership
 *     (via parent), and communication with the client through update instructions and event
 *     handling.
 * </p>
 *
 * <p>
 *     Every widget is associated with a {@link Client} instance and is assigned a globally
 *     unique {@link UId}. Widgets automatically register themselves in the client's widget
 *     registry upon creation.
 * </p>
 *
 * <p>
 *     This class is intended to be subclassed to implement concrete widget types such as buttons,
 *     input fields, containers, etc.
 * </p>
 */
public abstract class Widget {

    /**
     * Unique identifier of this widget.
     */
    private final UId widgetId;

    /**
     * The client to which this widget belongs.
     */
    private final Client client;

    /**
     * Parent (owning container) of this widget.
     */
    private final Container parent;

    /**
     * Constructs a new widget and registers it with the associated client.
     * <p>
     *     A {@code create} instruction is automatically added to the update queue.
     * </p>
     *
     * @param client The owning client instance
     * @param parent The container that owns this widget (nullable for root)
     */
    public Widget(final Client client, final Container parent) {
        this.widgetId = UId.create();
        this.client = client;
        this.client.widgets.put(this.widgetId, this);
        this.parent = parent;
        client.addInstruction(new Create(this.widgetId, this.getType()));
    }

    /**
     * Returns the unique identifier assigned to this widget.
     *
     * @return Widget ID
     */
    UId getWidgetId() {
        return this.widgetId;
    }

    /**
     * Returns the client that owns this widget.
     *
     * @return Associated {@link Client} instance
     */
    public Client getClient() {
        return this.client;
    }

    /**
     * Returns the parent container of this widget.
     *
     * @return Parent container, or {@code null} if this is the root
     */
    public Container getParent() {
        return this.parent;
    }

    /**
     * Accepts a visitor object that performs operations on this widget.
     * <p>
     *     Part of the visitor pattern for separating widget structure from logic.
     * </p>
     *
     * @param visitor The visitor to accept
     * @return Result of the visitor's operation
     */
    public abstract boolean accept(final WidgetVisitor visitor);

    /**
     * Returns the type name of this widget.
     * <p>
     *     Used when generating client-side create instructions. The type is typically a string
     *     like {@code "button"}, {@code "input"}, etc.
     * </p>
     *
     * @return Widget type string
     */
    abstract String getType();

    /**
     * Handles an event sent from the client.
     * <p>
     *     Called when the frontend sends a user action event associated with this widget.
     *     Subclasses should override this to implement specific behavior for different event types.
     * </p>
     *
     * @param type Event type string (e.g., "click", "change")
     * @param data Optional JSON payload with additional event details
     */
    abstract void handleEvent(final String type, final Optional<JsonObject> data);

    /**
     * Queues an instruction to be sent to the client during the next update cycle.
     *
     * @param instruction the instruction to add
     */
    void sendToClient(final Instruction instruction) {
        this.client.addInstruction(instruction);
    }

    /**
     * Removes this widget from the UI and its parent container.
     * <p>
     *     The widget is removed from the parent container (if any),
     *     and a {@code Remove} instruction is sent to the client.
     * </p>
     */
    public void remove() {
        if (this.parent != null) {
            this.parent.remove(this);
        }
        this.client.widgets.remove(this.widgetId);
        //this.sendToClient(new Remove(this.widgetId));
    }
}
