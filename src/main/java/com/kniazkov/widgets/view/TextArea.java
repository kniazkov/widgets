/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * A multi-line text input area widget for longer text entries.
 * Extends {@link InputField} to provide similar functionality with multi-line support.
 */
public class TextArea extends InputField {

    /**
     * Creates a new text area with empty text.
     */
    public TextArea() {
        super();
    }

    /**
     * Creates a new text area with the given initial text.
     *
     * @param text the initial text to display in the text area
     */
    public TextArea(final String text) {
        super(text);
    }

    /**
     * Creates a new text area with the specified style and initial text.
     *
     * @param style the style to apply to this widget
     * @param text the initial text to display
     */
    public TextArea(final InputFieldStyle style, final String text) {
        super(style, text);
    }

    @Override
    public String getType() {
        return "text area";
    }
}
