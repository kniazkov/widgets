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
 * Inline container that places all of its children into the same visual area.
 *
 * <p>The first child forms the bottom layer. Every subsequent child is painted
 * above the preceding children. The stack occupies the maximum width and height
 * required by its children.</p>
 */
public final class OverlayStack extends InlineWidget<InlineBlockStyle>
        implements TypedContainer<Widget<?>>, HasBgColor, HasBorder,
        HasWidth, HasHeight, HasMargin, HasPadding, HandlesPointerEvents,
        HasBoxShadow, HasCursor, HasTransition, HasBoxSizing {
    /** Child widgets ordered from the bottom layer to the top layer. */
    private final List<Widget<?>> children = new ArrayList<>();

    /**
     * Returns the default style used by overlay stacks.
     *
     * @return default inline-block style
     */
    public static InlineBlockStyle getDefaultStyle() {
        return InlineBlock.getDefaultStyle();
    }

    /** Creates an empty overlay stack with the default style. */
    public OverlayStack() {
        super(getDefaultStyle());
    }

    /**
     * Creates an overlay stack containing the specified widgets.
     *
     * @param children layers ordered from bottom to top
     */
    public OverlayStack(final Widget<?>... children) {
        this(getDefaultStyle(), children);
    }

    /**
     * Creates an empty overlay stack with the specified style.
     *
     * @param style stack style
     */
    public OverlayStack(final InlineBlockStyle style) {
        super(style);
    }

    /**
     * Creates an overlay stack with the specified style and layers.
     *
     * @param style stack style
     * @param children layers ordered from bottom to top
     */
    @SuppressWarnings("this-escape")
    public OverlayStack(
        final InlineBlockStyle style,
        final Widget<?>... children
    ) {
        super(style);
        for (final Widget<?> child : children) {
            this.appendChild(child);
        }
    }

    @Override
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public Widget<?> getChild(final int index)
            throws IndexOutOfBoundsException {
        return this.children.get(index);
    }

    @Override
    public void add(final Widget<?> widget) {
        this.appendChild(widget);
    }

    /**
     * Appends a layer without dispatching to an overridable method from a
     * constructor.
     *
     * @param widget new top layer
     */
    private void appendChild(final Widget<?> widget) {
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
        return "overlay stack";
    }
}
