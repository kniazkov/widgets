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
     * @param client Client that owns this widget
     */
    RootWidget(final @NotNull Client client) {
        this.children = new ArrayList<>();
        this.client.setData(client);
        client.widgets.put(this.getWidgetId(), this);
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

    @Override
    public RootWidget appendChild(final @NotNull BlockWidget widget) {
        this.children.add(widget);
        widget.setParent(this);
        this.sendToClient(new AppendChild(this.getWidgetId(), widget.getWidgetId()));
        return this;
    }
}
