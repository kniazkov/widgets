/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Represents standard horizontal alignment options used for inline and block-level content.
 * <p>
 * These values correspond directly to CSS keywords for the {@code text-align} property,
 * such as {@code left}, {@code center}, or {@code right}.
 */
public enum HorizontalAlignment {
    /**
     * Aligns content to the left.
     * CSS value: {@code left}.
     */
    LEFT("left"),

    /**
     * Aligns content to the center.
     * CSS value: {@code center}.
     */
    CENTER("center"),

    /**
     * Aligns content to the right.
     * CSS value: {@code right}.
     */
    RIGHT("right");

    /**
     * The CSS keyword representing this alignment.
     */
    private final String cssCode;

    /**
     * Constructor.
     *
     * @param cssCode the CSS keyword for this alignment
     */
    HorizontalAlignment(final String cssCode) {
        this.cssCode = cssCode;
    }

    /**
     * Returns the CSS-compatible string for this alignment.
     *
     * @return the CSS keyword representing this alignment
     */
    public String getCSSCode() {
        return this.cssCode;
    }

    @Override
    public String toString() {
        return this.cssCode;
    }

    /**
     * Parses a string and returns the corresponding {@code HorizontalAlignment}.
     * Matching is case-insensitive. If the value is not recognized,
     * {@link #LEFT} is returned.
     *
     * @param value the CSS alignment keyword
     * @return parsed alignment or {@link #LEFT} if unknown
     */
    public static HorizontalAlignment fromString(final String value) {
        if (value == null) {
            return LEFT;
        }
        final String normalized = value.trim().toLowerCase();
        for (final HorizontalAlignment align : values()) {
            if (align.cssCode.equals(normalized)) {
                return align;
            }
        }
        return LEFT;
    }
}
