/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The root widget, that is, the one that contains all other widgets and has no parent.
 * The root widget has no public constructor and is created by the library when the client requests
 * a new page instance.
 */
public final class RootWidget extends Widget implements Container<BlockWidget> {
    /**
     * Child widgets.
     */
    private final List<BlockWidget> children;

    /**
     * Constructor.
     */
    RootWidget() {
        this.children = new ArrayList<>();
    }

    @Override
    void handleEvent(@NotNull JsonObject json, @NotNull String type) {
        // do nothing for now
    }

    @Override
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public @NotNull BlockWidget getChild(final int index) throws IndexOutOfBoundsException {
        return this.children.get(index);
    }

    /**
     * Adds a widget as the last widget to the root widget.
     * @param widget Widget
     */
    public void appendChild(final @NotNull BlockWidget widget) {
        this.children.add(widget);
    }
}
