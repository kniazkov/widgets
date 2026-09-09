/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.protocol.AppendChild;
import com.kniazkov.widgets.protocol.RemoveChild;
import java.util.ArrayList;
import java.util.List;

/**
 * A non-modal popup window fixed to the browser viewport.
 * It stays visible without blocking the rest of the page until application code removes it.
 */
public class Popup extends BlockWidget<PopupStyle> implements BlockContainer,
        HasBgColor, HasBorder, HasWidth, HasHeight, HasPadding, HasBoxShadow,
        HasBoxSizing, HasHorizontalAlignment, HasVerticalAlignment {
    /**
     * Child widgets.
     */
    private final List<BlockWidget<?>> children = new ArrayList<>();

    /**
     * Returns the global default popup style.
     *
     * @return default popup style
     */
    public static PopupStyle getDefaultStyle() {
        return PopupStyle.DEFAULT;
    }

    /**
     * Creates an empty popup with the default style.
     */
    public Popup() {
        this(getDefaultStyle());
    }

    /**
     * Creates a popup containing the specified block widgets.
     *
     * @param children initial children
     */
    public Popup(final BlockWidget<?>... children) {
        this(getDefaultStyle(), children);
    }

    /**
     * Creates an empty popup with the specified style.
     *
     * @param style popup style
     */
    public Popup(final PopupStyle style) {
        super(style);
    }

    /**
     * Creates a popup with the specified style and children.
     *
     * @param style popup style
     * @param children initial children
     */
    @SuppressWarnings("this-escape")
    public Popup(final PopupStyle style, final BlockWidget<?>... children) {
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
     * @param widget child widget
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
        return "popup";
    }
}
