/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Indeterminate progress for an operation whose total work is unknown.
 */
public final class Busy extends Progress {
    /**
     * Creates a running indeterminate indicator.
     * @param text localized operation label
     */
    public Busy(final String text) {
        super(text);
    }
}
