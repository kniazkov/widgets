package com.kniazkov.widgets;

import java.util.Objects;

/**
 * Represents an absolute size (width or height) of an inline widget.
 * <p>
 *     This value is expressed in pixels internally, parsed from a CSS-style string.
 *     Only absolute units are supported:
 * </p>
 * <ul>
 *     <li>{@code px} — pixels (default if no unit specified)</li>
 *     <li>{@code pt} — points (1pt = 1/72 inch)</li>
 *     <li>{@code pc} — picas (1pc = 12pt)</li>
 *     <li>{@code in} — inches</li>
 *     <li>{@code cm} — centimeters</li>
 *     <li>{@code mm} — millimeters</li>
 * </ul>
 *
 * <p>
 *     The minimum accepted value is clamped to 1 pixel.
 * </p>
 */
public class InlineWidgetSize implements WidgetSize, Comparable<InlineWidgetSize> {
    /**
     * Singleton instance representing an undefined size.
     * Used when no explicit value has been assigned.
     */
    public static final InlineWidgetSize UNDEFINED = new InlineWidgetSize(0) {
        @Override
        public String getCSSCode() {
            return "";
        }

        @Override
        public String toString() {
            return "undefined";
        }
    };

    /**
     * Internal size in pixels.
     */
    private final int pixels;

    /**
     * Constructs a new size in pixels.
     *
     * @param px Size in pixels; must be ≥ 0
     * @throws IllegalArgumentException if the size is negative
     */
    public InlineWidgetSize(final int px) {
        if (px < 0) {
            throw new IllegalArgumentException("InlineWidgetSize must be ≥ 0, or use UNDEFINED");
        }
        this.pixels = px;
    }

    /**
     * Constructs a size by parsing a CSS-like string.
     * <p>
     *     Accepts units like {@code px}, {@code pt}, {@code in}, etc.
     *     If no unit is provided, pixels are assumed.
     * </p>
     *
     * @param str Size string (e.g., "10pt", "24px", "1in")
     */
    public InlineWidgetSize(String str) {
        str = str.trim().toLowerCase();
        if (str.isEmpty()) {
            throw new IllegalArgumentException("Empty size string");
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

        this.pixels = Math.max(0, (toPixels(value, unit)));
    }

    /**
     * Converts a given numeric value with unit to pixels.
     *
     * @param value Numeric portion of the input
     * @param unit  Unit suffix (e.g., "pt", "px", etc.)
     * @return Converted pixel value
     * @throws IllegalArgumentException If the unit is unsupported
     */
    private static int toPixels(final float value, final String unit) {
        switch (unit) {
            case "px": return Math.round(value);
            case "pt": return Math.round(value * 96f / 72f);
            case "pc": return Math.round(value * 16f);
            case "in": return Math.round(value * 96f);
            case "cm": return Math.round(value * 96f / 2.54f);
            case "mm": return Math.round(value * 96f / 25.4f);
            default: throw new IllegalArgumentException("Unsupported unit: " + unit);
        }
    }

    @Override
    public String getCSSCode() {
        return this.pixels + "px";
    }

    @Override
    public String toString() {
        return this.getCSSCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof InlineWidgetSize && ((InlineWidgetSize) obj).pixels == this.pixels;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.pixels);
    }

    @Override
    public int compareTo(final InlineWidgetSize other) {
        return Integer.compare(this.pixels, other.pixels);
    }
}
