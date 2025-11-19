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

    /**
     * Parses a CSS-style size string (e.g. "10px", "12pt", "1in", "100%").
     *
     * @param str input string
     * @return parsed size
     * @throws IllegalArgumentException if the string is invalid
     */
    static WidgetSize parse(final String str) {
        final String s = str.trim().toLowerCase();
        if (s.isEmpty()) {
            return AbsoluteSize.UNDEFINED;
        }

        // find start of unit suffix
        int i = 0;
        while (i < s.length() &&
               (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
            i++;
        }

        if (i == 0) {
            throw new IllegalArgumentException("Invalid size format: " + str);
        }

        final float value = Float.parseFloat(s.substring(0, i));
        final String substr = s.substring(i).trim();

        final Unit unit;
        switch (substr) {
            case "pt": unit = Unit.PT; break;
            case "pc": unit = Unit.PC; break;
            case "in": unit = Unit.IN; break;
            case "cm": unit = Unit.CM; break;
            case "mm": unit = Unit.MM; break;
            case "px":
            case ""  : unit = Unit.PX; break;
            case "%" :
                return new RelativeSize(value);
            default:
                throw new IllegalArgumentException("Unsupported unit: " + substr);
        }

        return new AbsoluteSize(value, unit);
    }
}
