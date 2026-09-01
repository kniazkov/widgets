/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Defines how content that extends beyond a widget's box is displayed.
 */
public enum Overflow {
    /**
     * Overflowing content remains visible.
     */
    VISIBLE("visible"),
    /**
     * Overflowing content is clipped without scroll bars.
     */
    HIDDEN("hidden"),
    /**
     * Overflowing content is clipped at the overflow clip edge.
     */
    CLIP("clip"),
    /**
     * Scroll bars are always available.
     */
    SCROLL("scroll"),
    /**
     * Scroll bars are added when the content overflows.
     */
    AUTO("auto");

    /**
     * CSS keyword.
     */
    private final String cssCode;

    /**
     * Creates an overflow value.
     *
     * @param cssCode CSS keyword
     */
    Overflow(final String cssCode) {
        this.cssCode = cssCode;
    }

    /**
     * Returns the CSS keyword.
     *
     * @return CSS overflow value
     */
    public String getCSSCode() {
        return this.cssCode;
    }

    @Override
    public String toString() {
        return this.cssCode;
    }
}
