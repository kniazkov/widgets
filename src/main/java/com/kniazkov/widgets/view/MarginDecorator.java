/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.protocol.SetChild;

/**
 * A decorator widget that adds margin properties to inline widgets that don't
 * natively support them.
 * <p>
 * This decorator wraps an existing inline widget and provides margin capabilities,
 * allowing visual separation of inline elements (like text fragments) without modifying
 * the original widget's implementation. Useful when you need to add spacing to inline
 * elements that don't have built-in margin support.
 */
public class MarginDecorator extends InlineWidget implements Decorator<InlineWidget>, HasMargin {
    /**
     * Decorated widget.
     */
    private InlineWidget child;

    /**
     * Constructs a margin decorator wrapping the specified inline widget.
     *
     * @param child the inline widget to decorate with margin properties
     */
    public MarginDecorator(final InlineWidget child) {
        super(Style.getEmptyStyle());
        this.child = child;
        this.pushUpdate(new SetChild(this.child.getId(), this.getId()));
    }

    @Override
    public InlineWidget getChild() {
        return this.child;
    }

    @Override
    public void put(final InlineWidget widget) {
        if (this.child == widget) {
            return;
        }
        this.child.setParent(null);
        this.child = widget;
        widget.setParent(this);
        this.pushUpdate(new SetChild(this.child.getId(), this.getId()));
    }

    @Override
    public void remove(final Widget widget) {
        if (widget != this.child) {
            return;
        }
        final InlineWidget old = this.child;
        this.child = null;
        old.setParent(null);
        this.child = new TextWidget();
        this.pushUpdate(new SetChild(this.child.getId(), this.getId()));
    }

    @Override
    public String getType() {
        return "margin decorator";
    }
}
