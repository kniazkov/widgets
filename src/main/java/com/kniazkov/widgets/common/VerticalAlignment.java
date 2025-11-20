/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Represents standard vertical alignment options used for inline and block-level content.
 * <p>
 * These values correspond to CSS keywords used for vertical alignment,
 * such as {@code top}, {@code middle}, or {@code bottom}.
 */
public enum VerticalAlignment {
    /**
     * Aligns content to the top.
     * CSS value: {@code top}.
     */
    TOP("top"),

    /**
     * Aligns content to the middle.
     * CSS value: {@code middle}.
     */
    MIDDLE("middle"),

    /**
     * Aligns content to the bottom.
     * CSS value: {@code bottom}.
     */
    BOTTOM("bottom");

    /**
     * The CSS keyword representing this alignment.
     */
    private final String cssCode;

    /**
     * Constructor.
     *
     * @param cssCode the CSS keyword for this alignment
     */
    VerticalAlignment(final String cssCode) {
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
     * Parses a string and returns the corresponding {@code VerticalAlignment}.
     * Matching is case-insensitive. If the value is not recognized,
     * {@link #TOP} is returned.
     *
     * @param value the CSS vertical alignment keyword
     * @return parsed alignment or {@link #TOP} if unknown
     */
    public static VerticalAlignment fromString(final String value) {
        if (value == null) {
            return TOP;
        }
        final String normalized = value.trim().toLowerCase();
        for (final VerticalAlignment align : values()) {
            if (align.cssCode.equals(normalized)) {
                return align;
            }
        }
        return TOP;
    }
}
