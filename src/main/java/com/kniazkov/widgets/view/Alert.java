/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Persistent localized status or error message.
 */
public final class Alert extends Feedback {
    /**
     * Creates a localized message. Empty text hides the message itself.
     * @param text plain text
     */
    public Alert(final String text) {
        super("alert", text, Severity.INFO);
    }
}
