/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence;

/**
 * A commit may be durable but could not be fully installed. Close and reopen the
 * database before reading or writing again; never blindly retry a business operation.
 */
public final class RecoveryRequiredException extends IllegalStateException {
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param message diagnostic
     * @param cause storage failure
     */
    public RecoveryRequiredException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
