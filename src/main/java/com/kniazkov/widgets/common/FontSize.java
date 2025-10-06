package com.kniazkov.widgets.common;

import java.util.Objects;

/**
 * Represents a CSS-compatible font size using absolute units.
 * Internally stores the value in <b>pixels</b> and supports parsing from strings
 * like {@code "12pt"}, {@code "1in"}, {@code "14px"}, etc. Supported units:
 * <ul>
 *     <li>{@code px} — pixels</li>
 *     <li>{@code pt} — points (1pt = 1/72 inch)</li>
 *     <li>{@code pc} — picas (1pc = 12pt)</li>
 *     <li>{@code in} — inches</li>
 *     <li>{@code cm} — centimeters</li>
 *     <li>{@code mm} — millimeters</li>
 * </ul>
 * If no unit is provided when parsing from string, {@code px} is assumed.
 * The minimum resolved pixel value is always clamped to 3.
 */
public final class FontSize implements Comparable<FontSize> {
    /**
     * Default font size used when none is explicitly specified.
     * Equivalent to {@code 12pt} or {@code 16px}.
     */
    public static final FontSize DEFAULT = new FontSize("12pt");

    /**
     * Internal pixel value.
     */
    private final int pixels;

    /**
     * Constructs a font size from a pixels value.

     * @param px size in pixels
     */
    public FontSize(int px) {
        this.pixels = px;
    }

    /**
     * Constructs a font size from a CSS-style string such as "12pt", "1in", or "16px".
     * If no unit is specified, pixels are assumed.
     *
     * @param str font size string to parse
     * @throws IllegalArgumentException if the input is invalid or contains unsupported units
     */
    public FontSize(String str) {
        str = str.trim().toLowerCase();
        if (str.isEmpty()) {
            throw new IllegalArgumentException("Empty font size");
        }

        int unitStart = 0;
        while (unitStart < str.length() &&
                (Character.isDigit(str.charAt(unitStart)) || str.charAt(unitStart) == '.')) {
            unitStart++;
        }

        float value = Float.parseFloat(str.substring(0, unitStart));
        String unit = str.substring(unitStart).trim();
        if (unit.isEmpty()) {
            unit = "px";
        }

        this.pixels = clamp(toPixels(value, unit));
    }

    /**
     * Converts a given numeric value with unit to pixels.
     *
     * @param value numeric portion of the input
     * @param unit  unit suffix (e.g., "pt", "px", etc.)
     * @return converted pixel value
     * @throws IllegalArgumentException If the unit is unsupported
     */
    private static int toPixels(final float value, final String unit) {
        switch (unit) {
            case "px": return Math.round(value);
            case "pt": return Math.round(value * 96f / 72f);
            case "pc": return Math.round(value * 16f); // 1pc = 12pt = 16px
            case "in": return Math.round(value * 96f);
            case "cm": return Math.round(value * 96f / 2.54f);
            case "mm": return Math.round(value * 96f / 25.4f);
            default: throw new IllegalArgumentException("Unsupported unit: " + unit);
        }
    }

    /**
     * Ensures that the pixel value is not below 3.
     *
     * @param px pixel value
     * @return clamped pixel value (minimum 3)
     */
    private static int clamp(int px) {
        return Math.max(3, px);
    }

    /**
     * Returns the size as a CSS-compatible string.
     *
     * @return CSS size string (e.g., {@code "16px"})
     */
    public String getCSSCode() {
        return this.pixels + "px";
    }

    @Override
    public String toString() {
        return this.getCSSCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof FontSize && ((FontSize) obj).pixels == this.pixels;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.pixels);
    }

    @Override
    public int compareTo(final FontSize other) {
        return Integer.compare(this.pixels, other.pixels);
    }
}
