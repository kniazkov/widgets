/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.protocol.AppendChild;
import com.kniazkov.widgets.protocol.RemoveChild;
import java.util.ArrayList;
import java.util.List;

/**
 * An atomic inline group of inline widgets with no automatic line wrapping.
 * The whole group can move to the next line in a Section, but its contents stay together.
 * An oversized group overflows rather than wrapping or shrinking its text.
 * Explicit line breaks and layouts inside composite children are not overridden.
 */
public class NoWrap extends InlineWidget<NoWrapStyle> implements TypedContainer<InlineWidget<?>>,
        HasMargin, HasPadding,
        HasHiddenState {
    /**
     * Returns the default style instance used by no-wraps.
     *
     * @return the singleton default {@link NoWrapStyle} instance
     */
    public static NoWrapStyle getDefaultStyle() {
        return NoWrapStyle.DEFAULT;
    }

    /**
     * List of child widgets.
     */
    private final List<InlineWidget<?>> children = new ArrayList<>();

    /**
     * Creates a new no-wrap.
     */
    public NoWrap() {
        this(getDefaultStyle());
    }

    /**
     * Creates a new no-wrap containing the specified inline widgets.
     *
     * @param children the initial child widgets, in display order
     */
    public NoWrap(final InlineWidget<?>... children) {
        this(getDefaultStyle(), children);
    }

    /**
     * Creates a new no-wrap with specified style.
     *
     * @param style no-wrap style
     */
    public NoWrap(final NoWrapStyle style) {
        super(style);
    }

    /**
     * Creates a new no-wrap with the specified style and child widgets.
     *
     * @param style no-wrap style
     * @param children the initial child widgets, in display order
     */
    @SuppressWarnings("this-escape")
    public NoWrap(final NoWrapStyle style, final InlineWidget<?>... children) {
        /*
         * Construction attaches the initial children before this instance is published.
         */
        super(style);
        for (final InlineWidget<?> child : children) {
            this.appendChild(child);
        }
    }

    @Override
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public InlineWidget<?> getChild(final int index) throws IndexOutOfBoundsException {
        return this.children.get(index);
    }

    @Override
    public void remove(final Widget<?> widget) {
        if (this.children.remove(widget)) {
            this.pushUpdate(new RemoveChild(widget.getId(), this.getId()));
            widget.setParent(null);
        }
    }

    @Override
    public void add(final InlineWidget<?> widget) {
        this.appendChild(widget);
    }

    /**
     * Appends a child without dispatching to an overridable method from a constructor.
     *
     * @param widget the child widget
     */
    private void appendChild(final InlineWidget<?> widget) {
        this.children.add(widget);
        widget.setParent(this);
        pushUpdate(new AppendChild(widget.getId(), this.getId()));
    }

    @Override
    public String getType() {
        return "no-wrap";
    }
}
