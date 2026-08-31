/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.HandlesPointerEvents;
import com.kniazkov.widgets.protocol.AppendChild;
import com.kniazkov.widgets.protocol.RemoveChild;
import java.util.ArrayList;
import java.util.List;

/**
 * A general-purpose block-level container for composing groups of block widgets.
 * A panel is rendered as a block element and may itself be nested in any
 * {@link BlockContainer}.
 */
public class Panel extends BlockWidget<PanelStyle> implements BlockContainer,
        HasBgColor, HasBorder, HasWidth, HasHeight, HasMargin, HasPadding,
        HandlesPointerEvents, HasBoxShadow, HasCursor, HasTransition, HasBoxSizing {
    /**
     * Returns the default style instance used by panels.
     *
     * @return the singleton default {@link PanelStyle} instance
     */
    public static PanelStyle getDefaultStyle() {
        return PanelStyle.DEFAULT;
    }

    /**
     * Child widgets.
     */
    private final List<BlockWidget<?>> children = new ArrayList<>();

    /**
     * Creates an empty panel with the default style.
     */
    public Panel() {
        super(getDefaultStyle());
    }

    /**
     * Creates a panel containing the specified block widgets.
     *
     * @param children the initial child widgets, in display order
     */
    public Panel(final BlockWidget<?>... children) {
        this(getDefaultStyle(), children);
    }

    /**
     * Creates an empty panel with the specified style.
     *
     * @param style the panel style to use
     */
    public Panel(final PanelStyle style) {
        super(style);
    }

    /**
     * Creates a panel with the specified style and child widgets.
     *
     * @param style the panel style to use
     * @param children the initial child widgets, in display order
     */
    @SuppressWarnings("this-escape")
    public Panel(final PanelStyle style, final BlockWidget<?>... children) {
        /*
         * Construction attaches the initial children before this instance is published.
         */
        super(style);
        for (final BlockWidget<?> child : children) {
            this.appendChild(child);
        }
    }

    @Override
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public BlockWidget<?> getChild(final int index) throws IndexOutOfBoundsException {
        return this.children.get(index);
    }

    @Override
    public void add(final BlockWidget<?> widget) {
        this.appendChild(widget);
    }

    /**
     * Appends a child without dispatching to an overridable method from a constructor.
     *
     * @param widget the child widget
     */
    private void appendChild(final BlockWidget<?> widget) {
        this.children.add(widget);
        widget.setParent(this);
        this.pushUpdate(new AppendChild(widget.getId(), this.getId()));
    }

    @Override
    public void remove(final Widget<?> widget) {
        if (this.children.remove(widget)) {
            this.pushUpdate(new RemoveChild(widget.getId(), this.getId()));
            widget.setParent(null);
        }
    }

    @Override
    public String getType() {
        return "panel";
    }
}
