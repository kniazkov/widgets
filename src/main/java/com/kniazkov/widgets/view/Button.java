/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.HandlesPointerEvents;
import com.kniazkov.widgets.protocol.SetChild;

/**
 * A clickable button widget that decorates a single {@link InlineWidget}.
 */
public class Button extends InlineWidget implements Decorator<InlineWidget>,
        HandlesPointerEvents, HasBgColor, HasBorder, HasAbsoluteWidth, HasAbsoluteHeight,
        HasMargin, HasPadding
{
    /**
     * Returns the default style instance used by buttons.
     *
     * @return the singleton default {@link ButtonStyle} instance
     */
    public static ButtonStyle getDefaultStyle() {
        return ButtonStyle.DEFAULT;
    }

    /**
     * Decorated widget (displayed inside the button).
     */
    private InlineWidget child;

    /**
     * Creates a button with an empty text child.
     */
    public Button() {
        this("");
    }

    /**
     * Creates a button with default style and {@link TextWidget} as its initial child.
     *
     * @param text the button text
     */
    public Button(final String text) {
        this(getDefaultStyle(), text);
    }

    /**
     * Creates a button with the specified style and {@link TextWidget} as its initial child.
     *
     * @param style the style to apply to this widget
     * @param text the button text
     */
    public Button(final ButtonStyle style, final String text) {
        super(style);
        this.child = new TextWidget(text);
        this.pushUpdate(new SetChild(this.child.getId(), this.getId()));
    }

    @Override
    public InlineWidget getChild() {
        return this.child;
    }

    @Override
    public void put(InlineWidget widget) {
        if (this.child == widget) {
            return;
        }
        this.child.setParent(null);
        this.child = widget;
        widget.setParent(this);
        this.pushUpdate(new SetChild(this.child.getId(), this.getId()));
    }

    @Override
    public void remove(Widget widget) {
        this.child.setParent(null);
        this.child = new TextWidget();
        this.pushUpdate(new SetChild(this.child.getId(), this.getId()));
    }

    @Override
    public String getType() {
        return "button";
    }

    /**
     * Sets a new widget style.
     *
     * @param style new widget style
     */
    public void setStyle(ButtonStyle style) {
        super.setStyle(style);
    }
}
