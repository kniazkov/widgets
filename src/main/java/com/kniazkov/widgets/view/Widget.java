/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.common.UId;
import com.kniazkov.widgets.protocol.CreateWidget;
import com.kniazkov.widgets.protocol.RemoveWidgetFromContainer;
import com.kniazkov.widgets.protocol.Subscribe;
import com.kniazkov.widgets.protocol.Update;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Abstract base class for all UI widgets. A widget represents a single element in the view tree.
 * Each widget has a unique {@link UId} and maintains a list of pending {@link Update}s that
 * describe how the client should create or modify its representation.
 * On creation, every widget automatically generates a {@link CreateWidget} update with its type.
 */
public abstract class Widget {
    /**
     * Widget unique Id.
     */
    private final UId id;

    /**
     * List of updates not yet sent to the client.
     */
    private final List<Update> updates;

    /**
     * The parent container that contains this widget.
     */
    private Container parent;

    /**
     * Constructs a new widget and schedules a {@link CreateWidget} update.
     */
    public Widget() {
        this.id = UId.create();
        this.updates = new ArrayList<>();
        this.updates.add(new CreateWidget(this.id, this.getType()));
    }

    /**
     * Returns the unique identifier of this widget.
     *
     * @return the widget ID
     */
    public UId getId() {
        return this.id;
    }

    /**
     * Returns the type of this widget, such as {@code "button"} or {@code "input field"}.
     *
     * @return the widget type
     */
    public abstract String getType();

    /**
     * Handles an event received from the client for this widget.
     *
     * @param type the event type
     * @param data optional event data
     */
    abstract void handleEvent(final String type, final Optional<JsonObject> data);

    /**
     * Sets the parent container of this widget.
     * If the widget was previously attached to another container,
     * a {@link RemoveWidgetFromContainer} update will be queued.
     *
     * @param container the new parent container (may be {@code null})
     */
    void setParent(final Container container) {
        if (this.parent != null) {
            this.updates.add(new RemoveWidgetFromContainer(this.id, this.parent.getId()));
        }
        this.parent = container;
    }

    /**
     * Removes this widget from its parent container.
     * This effectively removes the widget from the UI hierarchy. After removal, this widget
     * is considered "detached" - it will no longer receive updates or events from the client.
     */
    public void remove() {
        this.setParent(null);
    }

    /**
     * Collects and clears all pending updates from this widget, adding them
     * to the given set. This effectively drains the update queues.
     *
     * @param set the set to which updates are added
     */
    void getUpdates(final Set<Update> set) {
        set.addAll(this.updates);
        this.updates.clear();
    }

    /**
     * Adds an update for this widget.
     *
     * @param update the update to add
     */
    protected void pushUpdate(final Update update) {
        this.updates.add(update);
    }

    /**
     * Adds a {@link Subscribe} update to listen for the specified event.
     *
     * @param event the event type to subscribe to
     */
    protected void subscribeToEvent(final String event) {
        this.updates.add(new Subscribe(this.id, event));
    }
}
