/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Defines a decorative line applied to text.
 */
public enum TextDecoration {
    /**
     * No decorative line.
     */
    NONE("none"),
    /**
     * A line below the text.
     */
    UNDERLINE("underline"),
    /**
     * A line above the text.
     */
    OVERLINE("overline"),
    /**
     * A line through the text.
     */
    LINE_THROUGH("line-through");

    /** CSS keyword. */
    private final String cssCode;

    /**
     * Creates a text decoration value.
     *
     * @param cssCode CSS keyword
     */
    TextDecoration(final String cssCode) {
        this.cssCode = cssCode;
    }

    /**
     * Returns the CSS keyword.
     *
     * @return CSS text-decoration value
     */
    public String getCSSCode() {
        return this.cssCode;
    }

    @Override
    public String toString() {
        return this.cssCode;
    }
}
