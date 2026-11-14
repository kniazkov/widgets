/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.HandlesFocusEvents;
import com.kniazkov.widgets.controller.HandlesPointerEvents;
import com.kniazkov.widgets.protocol.SetChild;
import java.util.Objects;

/**
 * A clickable button widget that decorates a single {@link InlineWidget}.
 */
public class Button extends InlineWidget<ButtonStyle> implements Decorator<InlineWidget<?>>,
        HandlesFocusEvents, HandlesPointerEvents, HasBgColor, HasBorder, HasAbsoluteWidth,
        HasAbsoluteHeight, HasMargin, HasPadding, HasDisabledState, HasHiddenState, HasBoxShadow,
        HasOutline, HasCursor, HasTransition, HasBoxSizing {
    /**
     * Returns the default style instance used by buttons.
     *
     * @return the singleton default {@link ButtonStyle} instance
     */
    public static ButtonStyle getDefaultStyle() {
        return ButtonStyle.DEFAULT;
    }

    /**
     * Creates a native button with only an icon as visible content.
     * The caller supplies an unparented icon; the action name belongs to the button.
     * @param icon decorative icon displayed inside the button
     * @param name nonblank accessible action name
     * @return native button with ordinary Button behavior
     */
    public static Button icon(final ImageWidget icon, final String name) {
        return icon(getDefaultStyle(), icon, name);
    }

    /**
     * Creates a styled native icon button with an explicit action name.
     * @param style button style
     * @param icon decorative icon displayed inside the button
     * @param name nonblank accessible action name
     * @return styled native button
     */
    public static Button icon(final ButtonStyle style, final ImageWidget icon, final String name) {
        if (Objects.requireNonNull(name, "name").isBlank()) {
            throw new IllegalArgumentException("An icon button requires an accessible name");
        }
        icon.setDecorative(true);
        final Button button = new Button(style, icon);
        button.setAccessibleName(name);
        return button;
    }

    /**
     * Decorated widget (displayed inside the button).
     */
    private InlineWidget<?> child;

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
     * Creates a button with the specified inline widget as its child.
     *
     * @param child the widget displayed inside the button
     */
    public Button(final InlineWidget<?> child) {
        this(getDefaultStyle(), child);
    }

    /**
     * Creates a button with the specified style and {@link TextWidget} as its initial child.
     *
     * @param style the style to apply to this widget
     * @param text the button text
     */
    public Button(final ButtonStyle style, final String text) {
        this(style, new TextWidget(style.getDefaultTextStyle(), text));
    }

    /**
     * Creates a button with the specified style and inline child widget.
     *
     * @param style the style to apply to this widget
     * @param child the widget displayed inside the button
     */
    @SuppressWarnings("this-escape")
    public Button(final ButtonStyle style, final InlineWidget<?> child) {
        /*
         * Construction establishes the required parent link before this instance is published.
         */
        super(style);
        this.child = child;
        this.child.setParent(this);
        this.pushUpdate(new SetChild(this.child.getId(), this.getId()));
    }

    @Override
    public InlineWidget<?> getChild() {
        return this.child;
    }

    @Override
    public void put(final InlineWidget<?> widget) {
        if (this.child == widget) {
            return;
        }
        this.child.setParent(null);
        this.child = widget;
        widget.setParent(this);
        this.pushUpdate(new SetChild(this.child.getId(), this.getId()));
    }

    @Override
    public void remove(final Widget<?> widget) {
        if (widget != this.child) {
            return;
        }
        final InlineWidget<?> old = this.child;
        this.child = null;
        old.setParent(null);
        this.child = new TextWidget();
        this.child.setParent(this);
        this.pushUpdate(new SetChild(this.child.getId(), this.getId()));
    }

    @Override
    public String getType() {
        return "button";
    }
}
