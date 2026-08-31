/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Defines how padding and borders participate in an element's declared size.
 */
public enum BoxSizing {
    /**
     * Width and height describe the content only.
     */
    CONTENT_BOX("content-box"),
    /**
     * Width and height include padding and borders.
     */
    BORDER_BOX("border-box");

    /**
     * CSS keyword.
     */
    private final String cssCode;

    /**
     * Creates a box-sizing value.
     *
     * @param cssCode CSS keyword
     */
    BoxSizing(final String cssCode) {
        this.cssCode = cssCode;
    }

    /**
     * Returns the CSS keyword.
     *
     * @return CSS box-sizing value
     */
    public String getCSSCode() {
        return this.cssCode;
    }

    @Override
    public String toString() {
        return this.cssCode;
    }
}
