/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

/**
 * Indicates that a draft was based on an obsolete record revision.
 */
public final class ConflictException extends IllegalStateException {
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception.
     *
     * @param message details
     */
    public ConflictException(final String message) {
        super(message);
    }
}
