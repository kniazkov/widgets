/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The root widget, that is, the one that contains all other widgets and has no parent.
 * The root widget has no public constructor and is created by the library when the client requests
 * a new page instance.
 */
public final class RootWidget extends Widget implements TypedContainer<BlockWidget> {
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
    public boolean accept(final @NotNull WidgetVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    @NotNull String getType() {
        return "root";
    }

    @Override
    void handleEvent(final @NotNull String type, final @Nullable JsonObject data) {
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
        this.sendToClient(new AppendChild(this.getWidgetId(), widget.getWidgetId()));
    }

    /**
     * Collects updates from all widgets to send to the web page.
     * @return List of instruction
     */
    @NotNull List<Instruction> collectUpdates() {
        final List<Instruction> list = new ArrayList<>();
        DeepFirst.traverse(this, widget -> {
            widget.getUpdates(list);
            return true;
        });
        return list;
    }
}
