/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * A simple text widget that displays styled textual content.
 */
public class TextWidget extends InlineWidget implements HasStyledText, HasColor {
    /**
     * Returns the default style instance used by text widgets.
     *
     * @return the singleton default {@link TextWidgetStyle} instance
     */
    public static TextWidgetStyle getDefaultStyle() {
        return TextWidgetStyle.DEFAULT;
    }

    /**
     * Creates a new text widget with empty text.
     */
    public TextWidget() {
        this("");
    }

    /**
     * Creates a new text widget with the given initial text value and the default style.
     *
     * @param text the initial text to display
     */
    public TextWidget(final String text) {
        this(getDefaultStyle(), text);
    }

    /**
     * Creates a new text widget with the specified style and initial text.
     *
     * @param style the style to apply to this widget
     * @param text the initial text to display
     */
    public TextWidget(final TextWidgetStyle style, final String text) {
        super(style);
        this.setText(text);
    }

    /**
     * Creates a new text widget with the specified style and model.
     *
     * @param style the style to apply to this widget
     * @param model the model containing initial text
     */
    public TextWidget(final TextWidgetStyle style, final Model<String> model) {
        super(style);
        this.setTextModel(model);
    }

    @Override
    public String getType() {
        return "text";
    }

    /**
     * Sets a new widget style.
     *
     * @param style new widget style
     */
    public void setStyle(final TextWidgetStyle style) {
        super.setStyle(style);
    }
}
