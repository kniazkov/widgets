/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;
import com.kniazkov.widgets.protocol.Create;
import com.kniazkov.widgets.protocol.Subscribe;
import com.kniazkov.widgets.protocol.Update;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Abstract base class for all UI widgets. A widget represents a single element in the view tree.
 * Each widget has a unique {@link UId} and maintains a list of pending {@link Update}s that
 * describe how the client should create or modify its representation.
 * On creation, every widget automatically generates a {@link Create} update with its type.
 * Container widgets can override {@link #getChildWidgets()} to expose their children. Updates
 * from the entire widget tree can be collected using {@link #flush(Set)}.</p>
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
     * Constructs a new widget and schedules a {@link Create} update.
     */
    public Widget() {
        this.id = UId.create();
        this.updates = new ArrayList<>();
        this.updates.add(new Create(this.id, this.getType()));
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
    public  abstract String getType();

    /**
     * Returns the number of child widgets contained in this widget.
     *
     * @return the number of child widgets
     */
    public int getChildCount() {
        return 0;
    }

    /**
     * Returns the child widget at the given index.
     *
     * @param index the index of the child widget (from {@code 0} to {@code getChildCount() - 1})
     * @return the child widget
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public Widget getChild(final int index) {
        throw new IndexOutOfBoundsException();
    }

    /**
     * Handles an event received from the client for this widget.
     *
     * @param type the event type
     * @param data optional event data
     */
    public abstract void handleEvent(final String type, final Optional<JsonObject> data);

    /**
     * Collects and clears all pending updates from this widget and its children, adding them
     * to the given set. This effectively drains the update queues throughout the widget tree.
     *
     * @param set the set to which updates are added
     */
    public void flush(final Set<Update> set) {
        Deque<Widget> stack = new ArrayDeque<>();
        stack.push(this);
        while (!stack.isEmpty()) {
            final Widget widget = stack.pop();
            set.addAll(widget.updates);
            widget.updates.clear();
            final int count = widget.getChildCount();
            for (int index = 0; index < count; index++) {
                stack.push(widget.getChild(index));
            }
        }
    }

    /**
     * Adds an update for this widget.
     *
     * @param update the update to add
     */
    protected void update(final Update update) {
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
