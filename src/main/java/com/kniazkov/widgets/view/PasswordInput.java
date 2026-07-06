/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * A password input field widget.
 *
 * <p>This widget behaves like a regular {@link InputField}, but reports a
 * password-specific widget type so that the client can render it as a password
 * input field.
 */
public class PasswordInput extends InputField {
    /**
     * Creates a new password input field with empty text.
     */
    public PasswordInput() {
        super();
    }

    /**
     * Creates a new password input field with the given initial text.
     *
     * @param text the initial text to display in the password input field
     */
    public PasswordInput(final String text) {
        super(text);
    }

    /**
     * Creates a new password input field with the specified style and initial text.
     *
     * @param style the style to apply to this widget
     * @param text the initial text to display
     */
    public PasswordInput(final InputFieldStyle style, final String text) {
        super(style, text);
    }

    /**
     * Returns the widget type name used by the client-side renderer.
     *
     * @return the password input widget type
     */
    @Override
    public String getType() {
        return "password input";
    }
}
