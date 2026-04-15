/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Represents generic vertical alignment options independent of any specific
 * rendering technology or CSS property.
 * <p>
 * These values are intended to be serialized and sent to renderers, which
 * then map them to concrete platform-specific alignment rules.
 */
public enum VerticalAlignment {
    /**
     * Aligns content to the top.
     */
    TOP("top"),

    /**
     * Aligns content to the vertical center.
     */
    MIDDLE("middle"),

    /**
     * Aligns content to the bottom.
     */
    BOTTOM("bottom"),

    /**
     * Aligns content to the text baseline where supported.
     * Renderers that do not support baseline alignment should fall back to center alignment.
     */
    BASELINE("baseline");

    /**
     * Serialized alignment code used in protocol messages.
     */
    private final String code;

    /**
     * Constructor.
     *
     * @param code the serialized alignment code
     */
    VerticalAlignment(final String code) {
        this.code = code;
    }

    /**
     * Returns the serialized code of this alignment.
     *
     * @return the protocol code representing this alignment
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Returns the string representation of this alignment.
     *
     * @return the serialized alignment code
     */
    @Override
    public String toString() {
        return this.code;
    }

    /**
     * Parses a string and returns the corresponding {@code VerticalAlignment}.
     * Matching is case-insensitive. If the value is not recognized,
     * {@link #TOP} is returned.
     *
     * @param value the serialized alignment code
     * @return parsed alignment or {@link #TOP} if unknown
     */
    public static VerticalAlignment fromString(final String value) {
        if (value == null) {
            return TOP;
        }
        final String normalized = value.trim().toLowerCase();
        for (final VerticalAlignment alignment : values()) {
            if (alignment.code.equals(normalized)) {
                return alignment;
            }
        }
        return TOP;
    }
}
