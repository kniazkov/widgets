/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Represents an absolute size (width or height) of a widget.
 * <p>
 * Stores both the original numeric value and unit (e.g., {@code "10pt"}) along with a precomputed
 * pixel equivalent. This allows relative or incremental operations on sizes while maintaining
 * pixel precision for rendering on the client.
 * <p>
 * Supported units:
 * <ul>
 *     <li>{@code px} — pixels (default if no unit specified)</li>
 *     <li>{@code pt} — points (1pt = 1/72 inch)</li>
 *     <li>{@code pc} — picas (1pc = 12pt)</li>
 *     <li>{@code in} — inches</li>
 *     <li>{@code cm} — centimeters</li>
 *     <li>{@code mm} — millimeters</li>
 * </ul>
 * The minimum accepted value is clamped to 0 pixels.
 */
public class AbsoluteSize implements WidgetSize {
    /**
     * Undefined absolute size.
     */
    public static final AbsoluteSize UNDEFINED = new AbsoluteSize(0) {
        @Override
        public String getCSSCode() {
            return "";
        }

        @Override
        public String toString() {
            return "";
        }
    };

    /**
     * Original value.
     */
    private final float value;

    /**
     * Unit.
     */
    private final Unit unit;

    /**
     * Value in pixels.
     */
    private final int pixels;

    /**
     * Creates a size measured in pixels.
     *
     * @param px number of pixels (must be >= 0)
     */
    public AbsoluteSize(final int px) {
        if (px < 0) {
            throw new IllegalArgumentException("Pixel value must be >= 0");
        }
        this.value = px;
        this.unit = Unit.PX;
        this.pixels = px;
    }

    /**
     * Creates a size with explicit numeric value and unit.
     *
     * @param value numeric portion (must be >= 0)
     * @param unit  unit of measurement
     */
    public AbsoluteSize(final float value, final Unit unit) {
        if (value < 0f) {
            throw new IllegalArgumentException("Size value must be >= 0");
        }
        this.value = value;
        this.unit = unit;
        this.pixels = Math.max(0, toPixels(value, unit));
    }

    /**
     * Parses a CSS-style absolute size string (e.g. "10px", "12pt", "1in").
     *
     * @param str input string
     * @return parsed absolute size
     * @throws IllegalArgumentException if the string is invalid
     */
    public static AbsoluteSize parse(final String str) {
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
            default:
                throw new IllegalArgumentException("Unsupported unit: " + substr);
        }

        return new AbsoluteSize(value, unit);
    }

    /**
     * Converts a unit value to pixels.
     */
    private static int toPixels(final float value, final Unit unit) {
        switch (unit) {
            case PX: return Math.round(value);
            case PT: return Math.round(value * 96f / 72f);
            case PC: return Math.round(value * 16f);
            case IN: return Math.round(value * 96f);
            case CM: return Math.round(value * 96f / 2.54f);
            case MM: return Math.round(value * 96f / 25.4f);
            default:
                throw new IllegalArgumentException("Unsupported unit: " + unit);
        }
    }

    /**
     * Returns the original numeric value.
     *
     * @return the original numeric value
     */
    public float getValue() {
        return this.value;
    }

    /**
     * Returns the original unit.
     *
     * @return the original unit
     */
    public Unit getUnit() {
        return this.unit;
    }

    /**
     * Returns the computed pixel value.
     *
     * @return the computed pixel value
     */
    public int getPixels() {
        return this.pixels;
    }

    @Override
    public String getCSSCode() {
        return this.pixels + "px";
    }

    @Override
    public String toString() {
        return value + unit.name().toLowerCase();
    }
}
