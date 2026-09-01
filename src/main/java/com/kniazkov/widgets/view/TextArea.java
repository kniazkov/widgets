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
     * Default text area style derived from the regular input style.
     */
    private static final InputFieldStyle DEFAULT = createDefaultStyle();

    /**
     * Returns the default style instance used by text areas.
     *
     * @return the singleton default text area style
     */
    public static InputFieldStyle getDefaultStyle() {
        return DEFAULT;
    }

    /**
     * Creates a new text area with empty text.
     */
    public TextArea() {
        this("");
    }

    /**
     * Creates a new text area with the given initial text.
     *
     * @param text the initial text to display in the text area
     */
    public TextArea(final String text) {
        this(getDefaultStyle(), text);
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

    /**
     * Creates the larger default geometry required by a multi-line editor.
     *
     * @return text area default style
     */
    private static InputFieldStyle createDefaultStyle() {
        final InputFieldStyle style = InputField.getDefaultStyle().derive();
        style.setWidth(300);
        style.setHeight(96);
        return style;
    }
}
