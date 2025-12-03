/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.HandlesPointerEvents;

/**
 * An interactive text widget that displays styled textual content and responds
 * to user interactions.
 */

public class ActiveText extends InlineWidget implements HasStyledText, HasColor,
        HandlesPointerEvents
{
    /**
     * Returns the default style instance used by active text widgets.
     *
     * @return the singleton default {@link ActiveTextStyle} instance
     */
    public static ActiveTextStyle getDefaultStyle() {
        return ActiveTextStyle.DEFAULT;
    }

    /**
     * Creates a new active  text widget with empty text.
     */
    public ActiveText() {
        this("");
    }

    /**
     * Creates a new active text widget with the given initial text value and the default style.
     *
     * @param text the initial text to display
     */
    public ActiveText(final String text) {
        this(getDefaultStyle(), text);
    }

    /**
     * Creates a new  active text widget with the specified style and initial text.
     *
     * @param style the style to apply to this widget
     * @param text the initial text to display
     */
    public ActiveText(final ActiveTextStyle style, final String text) {
        super(style);
        this.setText(text);
    }

    @Override
    public String getType() {
        return "active text";
    }

    /**
     * Sets a new widget style.
     *
     * @param style new widget style
     */
    public void setStyle(final ActiveTextStyle style) {
        super.setStyle(style);
    }
}
