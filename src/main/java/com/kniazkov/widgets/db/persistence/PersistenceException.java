/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence;

/**
 * Reports a persistence failure without partially updating the in-memory state.
 */
public final class PersistenceException extends RuntimeException {
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception.
     *
     * @param message details
     * @param cause cause
     */
    public PersistenceException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception.
     *
     * @param message details
     */
    public PersistenceException(final String message) {
        super(message);
    }
}
