/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Browser keyboard hint; it does not validate or restrict input.
 */
public enum InputMode {
    /**
     * Default text keyboard.
     */
    TEXT,
    /**
     * Integer keyboard.
     */
    NUMERIC,
    /**
     * Decimal keyboard.
     */
    DECIMAL,
    /**
     * Telephone keyboard.
     */
    TEL,
    /**
     * Email keyboard.
     */
    EMAIL,
    /**
     * URL keyboard.
     */
    URL,
    /**
     * Search keyboard.
     */
    SEARCH,
    /**
     * Request no virtual keyboard.
     */
    NONE;

    /**
     * @return HTML inputmode value
     */
    public String getCode() {
        return this.name().toLowerCase(java.util.Locale.ROOT);
    }
}
