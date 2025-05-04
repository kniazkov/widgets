package com.kniazkov.widgets;

/**
 * Represents a widget size in CSS-compatible format.
 * <p>
 *     This abstraction is used for specifying dimensions such as width or height
 *     using absolute values (for example, {@code "100px"}).
 * </p>
 *
 * <p>
 *     If the size is undefined or not set, the implementation must return an empty string.
 *     In CSS, assigning an empty string resets the property to its default behavior.
 * </p>
 */
public interface WidgetSize {
    /**
     * Returns the size as a CSS-compatible string.
     * <p>
     *     If the size is undefined, this method returns an empty string.
     * </p>
     *
     * @return CSS size string (e.g., {@code "16px"}), or {@code ""} if undefined
     */
    String getCSSCode();
}
