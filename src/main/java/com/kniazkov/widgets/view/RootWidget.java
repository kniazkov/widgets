/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.base.Client;
import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.protocol.AppendChild;
import com.kniazkov.widgets.protocol.ResetClient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The root widget of a user interface hierarchy.
 * A {@code RootWidget} serves as the top-level container for all other widgets in the UI tree.
 * It manages a collection of {@link BlockWidget block widgets} that form the visible structure
 * of the interface. The root itself cannot have a parent container and represents the logical
 * entry point for traversing or updating the widget tree.
 */
public final class RootWidget extends Widget implements TypedContainer<BlockWidget> {
    /**
     * List of child widgets.
     */
    final List<BlockWidget> children = new ArrayList<>();

    /**
     * List of handlers that are called when the root node is closed.
     */
    final Set<Controller> closeHandlers = new HashSet<>();

    /**
     * Constructor.
     *
     * @param client client that owns this widget
     */
    public RootWidget(final Client client) {
        client.onClose(this::close);
    }

    @Override
    void setParent(final Container container) {
        if (container != null) {
            throw new IllegalArgumentException("The root widget cannot have a parent");
        }
    }

    @Override
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public BlockWidget getChild(int index) throws IndexOutOfBoundsException {
        return this.children.get(index);
    }

    @Override
    public void add(BlockWidget widget) {
        this.children.add(widget);
        widget.setParent(this);
        pushUpdate(new AppendChild(widget.getId(), this.getId()));
    }

    @Override
    public void remove(Widget widget) {
        if (this.children.remove(widget)) {
            widget.setParent(null);
        }
    }

    @Override
    public String getType() {
        return "root";
    }

    @Override
    public void handleEvent(String type, Optional<JsonObject> data) {
        // not yet
    }

    /**
     * Adds a handler that is called when the root node is destroyed.
     *
     * @param ctrl handler
     */
    public void onClose(Controller ctrl) {
        this.closeHandlers.add(ctrl);
    }

    @Override
    public void remove() {
        this.pushUpdate(new ResetClient());
    }

    /**
     * Invoked when the client is being closed.
     * This method performs the final shutdown procedure for the entire widget
     * hierarchy rooted at this widget. It first notifies all registered
     * {@link #closeHandlers controllers} by invoking their {@code handleEvent()}
     * method, allowing external components to react to the closing event
     * (e.g., to save state or release resources).
     * After notifying the controllers, the method traverses all child widgets
     * in the hierarchy and calls {@link Widget#detach()} on each of them.
     * This ensures that every widget releases its model bindings and unsubscribes
     * from any global models, preventing memory leaks caused by dangling listeners.
     */
    private void close() {
        for (final Controller ctrl : this.closeHandlers) {
            ctrl.handleEvent();
        }
        for (final Widget child : this) {
            child.detach();
        }
    }
}
