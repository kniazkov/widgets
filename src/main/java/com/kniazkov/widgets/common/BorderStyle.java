/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Represents standard CSS border styles used in web design and rendering engines.
 * <p>
 * These styles correspond directly to CSS keywords for the {@code border-style} property,
 * such as {@code solid}, {@code dashed}, or {@code dotted}.
 */
public enum BorderStyle {
    /**
     * No border is displayed.
     * CSS value: {@code none}.
     */
    NONE("none"),

    /**
     * A single solid line border.
     * CSS value: {@code solid}.
     */
    SOLID("solid"),

    /**
     * A double line border.
     * CSS value: {@code double}.
     */
    DOUBLE("double"),

    /**
     * A dotted line border.
     * CSS value: {@code dotted}.
     */
    DOTTED("dotted"),

    /**
     * A dashed line border.
     * CSS value: {@code dashed}.
     */
    DASHED("dashed");

    /**
     * The CSS keyword representing this border style.
     */
    private final String cssCode;

    /**
     * Constructor.
     *
     * @param cssCode the CSS keyword for this border style
     */
    BorderStyle(final String cssCode) {
        this.cssCode = cssCode;
    }

    /**
     * Returns the CSS-compatible string for this border style.
     * <p>
     * Example: {@code "solid"} or {@code "dotted"}.
     *
     * @return the CSS-compatible border style keyword
     */
    public String getCSSCode() {
        return this.cssCode;
    }

    @Override
    public String toString() {
        return this.cssCode;
    }

    /**
     * Parses a string and returns the corresponding {@code BorderStyle} value.
     * Matching is case-insensitive. If the string is not recognized,
     * {@link #NONE} is returned as the default.
     *
     * @param value the string representing a CSS border style
     * @return corresponding {@code BorderStyle} or {@link #NONE} if unknown
     */
    public static BorderStyle fromString(final String value) {
        if (value == null) {
            return NONE;
        }
        final String normalized = value.trim().toLowerCase();
        for (final BorderStyle style : values()) {
            if (style.cssCode.equals(normalized)) {
                return style;
            }
        }
        return NONE;
    }
}
