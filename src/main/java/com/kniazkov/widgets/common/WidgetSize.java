/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Represents a widget size in CSS-compatible format.
 * This abstraction is used for specifying dimensions such as width or height using absolute values
 * (for example, {@code "100px"}). If the size is undefined or not set, the implementation
 * must return an empty string. In CSS, assigning an empty string resets the property to its
 * default behavior.
 */
public interface WidgetSize {
    /**
     * Returns the size as a CSS-compatible string.
     * If the size is undefined, this method returns an empty string.
     *
     * @return CSS size string (e.g., {@code "16px"}), or {@code ""} if undefined
     */
    String getCSSCode();
}
