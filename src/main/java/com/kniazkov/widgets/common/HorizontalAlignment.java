/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Represents generic horizontal alignment options independent of any specific
 * rendering technology or CSS property.
 */
public enum HorizontalAlignment {
    /**
     * Aligns content to the left.
     */
    LEFT("left"),

    /**
     * Aligns content to the center.
     */
    CENTER("center"),

    /**
     * Aligns content to the right.
     */
    RIGHT("right"),

    /**
     * Distributes content evenly across the available width where supported.
     */
    JUSTIFY("justify");

    /**
     * Serialized alignment code used in protocol messages.
     */
    private final String code;

    /**
     * Constructor.
     *
     * @param code the serialized alignment code
     */
    HorizontalAlignment(final String code) {
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
     * Parses a string and returns the corresponding {@code HorizontalAlignment}.
     * Matching is case-insensitive. If the value is not recognized,
     * {@link #LEFT} is returned.
     *
     * @param value the serialized alignment code
     * @return parsed alignment or {@link #LEFT} if unknown
     */
    public static HorizontalAlignment fromString(final String value) {
        if (value == null) {
            return LEFT;
        }
        final String normalized = value.trim().toLowerCase();
        for (final HorizontalAlignment alignment : values()) {
            if (alignment.code.equals(normalized)) {
                return alignment;
            }
        }
        return LEFT;
    }
}