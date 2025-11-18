/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.protocol.AppendChild;
import com.kniazkov.widgets.protocol.ResetClient;
import java.util.ArrayList;
import java.util.List;

/**
 * The root widget of a user interface hierarchy.
 * A {@code RootWidget} serves as the top-level container for all other widgets in the UI tree.
 * It manages a collection of {@link BlockWidget block widgets} that form the visible structure
 * of the interface. The root itself cannot have a parent container and represents the logical
 * entry point for traversing or updating the widget tree.
 */
public final class RootWidget extends Widget implements TypedContainer<BlockWidget>,
        HasBgColor {
    /**
     * Returns the default style instance used by root widget.
     *
     * @return the singleton default {@link RootWidgetStyle} instance
     */
    public static RootWidgetStyle getDefaultStyle() {
        return RootWidgetStyle.DEFAULT;
    }

    /**
     * List of child widgets.
     */
    final List<BlockWidget> children = new ArrayList<>();

    /**
     * Constructor.
     *
     */
    public RootWidget() {
        super(getDefaultStyle());
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
    public void remove() {
        this.pushUpdate(new ResetClient());
    }

    /**
     * Sets a new widget style.
     *
     * @param style new widget style
     */
    public void setStyle(final RootWidgetStyle style) {
        super.setStyle(style);
    }
}
