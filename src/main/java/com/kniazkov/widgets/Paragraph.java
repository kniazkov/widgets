/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Widget that can contain text and other inline elements.
 */
public final class Paragraph extends BlockWidget implements TypedContainer<InlineWidget> {
    /**
     * Children widgets.
     */
    private final List<InlineWidget> children;

    /**
     * Constructor.
     */
    public Paragraph() {
        this.children = new ArrayList<>();
    }

    @Override
    public boolean accept(final @NotNull WidgetVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    @NotNull String getType() {
        return "paragraph";
    }

    @Override
    void handleEvent(final @NotNull JsonObject json, final @NotNull String type) {
        // do nothing for now
    }

    @Override
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public @NotNull InlineWidget getChild(final int index) throws IndexOutOfBoundsException {
        return this.children.get(index);
    }

    /**
     * Adds a widget as the last widget to the paragraph widget.
     * @param widget Widget
     */
    public void appendChild(final @NotNull InlineWidget widget) {
        this.children.add(widget);
        this.sendToClient(new AppendChild(this.getWidgetId(), widget.getWidgetId()));
    }
}
