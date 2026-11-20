/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

/**
 * A transport rejection with no submitted values in the exception or HTTP response.
 */
final class ProtocolAccessException extends RuntimeException {
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Whether a fresh document bootstrap is required.
     */
    private final boolean sessionExpired;

    /**
     * @param sessionExpired whether the browser's transport session has ended
     */
    ProtocolAccessException(final boolean sessionExpired) {
        super("Widgets protocol access denied");
        this.sessionExpired = sessionExpired;
    }

    /**
     * @return whether a fresh document bootstrap is required
     */
    boolean sessionExpired() {
        return this.sessionExpired;
    }
}
