/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Field validation message; bind with editor.setAccessibleError(error).
 */
public final class FieldError extends Feedback {
    /**
     * Creates a localized message. Empty text hides the message itself.
     * @param text plain text
     */
    public FieldError(final String text) {
        super("field-error", text, Severity.ERROR);
    }
}
