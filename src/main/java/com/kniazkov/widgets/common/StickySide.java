/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Selects the viewport edge used by a sticky widget while its containing block is scrolled.
 */
public enum StickySide {
    /**
     * Keeps the widget at the top edge of the viewport.
     */
    TOP("top"),

    /**
     * Keeps the widget at the bottom edge of the viewport.
     */
    BOTTOM("bottom");

    /**
     * Serialized value used by the browser protocol.
     */
    private final String code;

    /**
     * Creates a sticky side with its protocol value.
     *
     * @param code serialized value
     */
    StickySide(final String code) {
        this.code = code;
    }

    /**
     * Returns the value understood by the browser renderer.
     *
     * @return serialized side
     */
    public String getCode() {
        return this.code;
    }

    @Override
    public String toString() {
        return this.code;
    }
}
