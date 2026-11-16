/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Empty collection state with optional child action sections.
 */
public final class EmptyState extends Feedback {
    /**
     * Creates a localized message. Empty text hides the message itself.
     * @param text plain text
     */
    public EmptyState(final String text) {
        super("empty", text, Severity.INFO);
    }
}
