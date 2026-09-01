/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.HandlesFocusEvents;
import com.kniazkov.widgets.controller.HandlesPointerEvents;

/**
 * An interactive text hyperlink rendered as an HTML {@code <a>} element.
 */
public class Link extends InlineWidget<LinkStyle> implements HasStyledText, HasColor, HasHref,
        HandlesPointerEvents, HandlesFocusEvents, HasCursor, HasTransition {
    /**
     * Returns the default style instance used by links.
     *
     * @return the singleton default {@link LinkStyle} instance
     */
    public static LinkStyle getDefaultStyle() {
        return LinkStyle.DEFAULT;
    }

    /**
     * Creates an empty link pointing to {@code #}.
     */
    public Link() {
        this("");
    }

    /**
     * Creates a link with the specified text and the default {@code #} destination.
     *
     * @param text text displayed by the link
     */
    public Link(final String text) {
        this(getDefaultStyle(), text, "#");
    }

    /**
     * Creates a link with the specified text and destination.
     *
     * @param text text displayed by the link
     * @param href hyperlink destination
     */
    public Link(final String text, final String href) {
        this(getDefaultStyle(), text, href);
    }

    /**
     * Creates a link with the specified style and text, pointing to {@code #}.
     *
     * @param style style to apply to the link
     * @param text text displayed by the link
     */
    public Link(final LinkStyle style, final String text) {
        this(style, text, "#");
    }

    /**
     * Creates a link with the specified style, text, and destination.
     *
     * @param style style to apply to the link
     * @param text text displayed by the link
     * @param href hyperlink destination
     */
    @SuppressWarnings("this-escape")
    public Link(final LinkStyle style, final String text, final String href) {
        /*
         * Construction initializes inherited bindings before publication.
         */
        super(style);
        this.setText(text);
        this.setHref(href);
    }

    @Override
    public String getType() {
        return "link";
    }
}
