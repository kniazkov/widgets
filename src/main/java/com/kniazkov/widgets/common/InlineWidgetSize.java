package com.kniazkov.widgets.common;

import java.util.Locale;
import java.util.Objects;

/**
 * Represents an absolute size (width or height) of an inline widget.
 * <p>
 * Stores both the original numeric value and unit (e.g., {@code "10pt"})
 * along with a precomputed pixel equivalent. This allows relative or
 * incremental operations on sizes while maintaining pixel precision for
 * rendering on the client.
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
public class InlineWidgetSize implements WidgetSize, Comparable<InlineWidgetSize> {

    /**
     * Singleton instance representing an undefined size.
     * Used when no explicit value has been assigned.
     */
    public static final InlineWidgetSize UNDEFINED = new InlineWidgetSize(0, "px") {
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
     * Original numeric value (e.g., 12 from "12pt").
     */
    private final float value;

    /**
     * Original unit string (e.g., "pt", "px", etc.).
     */
    private final String unit;

    /**
     * Computed pixel equivalent (never negative).
     */
    private final int pixels;

    /**
     * Constructs a new inline widget size in pixels.
     *
     * @param px size in pixels; must be ≥ 0
     * @throws IllegalArgumentException if the size is negative
     */
    public InlineWidgetSize(final int px) {
        if (px < 0) {
            throw new IllegalArgumentException("InlineWidgetSize must be ≥ 0, or use UNDEFINED");
        }
        this.value = px;
        this.unit = "px";
        this.pixels = px;
    }

    /**
     * Constructs a new inline widget size from a numeric value and a unit string.
     *
     * @param value numeric portion (e.g. 12)
     * @param unit  unit suffix (e.g., "pt", "px", etc.)
     * @throws IllegalArgumentException if the value is negative or the unit unsupported
     */
    public InlineWidgetSize(final float value, final String unit) {
        if (value < 0) {
            throw new IllegalArgumentException("InlineWidgetSize must be ≥ 0, or use UNDEFINED");
        }
        if (unit == null || unit.isEmpty()) {
            throw new IllegalArgumentException("Unit must not be null or empty");
        }
        this.value = value;
        this.unit = unit.toLowerCase();
        this.pixels = Math.max(0, toPixels(value, this.unit));
    }

    /**
     * Constructs a new inline widget size by parsing a CSS-style string.
     *
     * @param str size string (e.g., "10pt", "24px", "1in")
     * @throws IllegalArgumentException if the input string is invalid or contains unsupported units
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

        this.value = Float.parseFloat(str.substring(0, unitStart));
        String parsedUnit = str.substring(unitStart).trim();
        if (parsedUnit.isEmpty()) {
            parsedUnit = "px";
        }

        this.unit = parsedUnit;
        this.pixels = Math.max(0, toPixels(this.value, this.unit));
    }

    /**
     * Converts a given numeric value with unit to pixels.
     *
     * @param value numeric portion of the input
     * @param unit  unit suffix (e.g., "pt", "px", etc.)
     * @return converted pixel value
     * @throws IllegalArgumentException if the unit is unsupported
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

    /**
     * Returns the numeric portion of the size.
     *
     * @return numeric value
     */
    public float getValue() {
        return this.value;
    }

    /**
     * Returns the unit portion of the size.
     *
     * @return unit string (e.g., "px", "pt", etc.)
     */
    public String getUnit() {
        return this.unit;
    }

    /**
     * Returns the computed pixel equivalent.
     *
     * @return size in pixels (≥ 0)
     */
    public int getPixels() {
        return this.pixels;
    }

    /**
     * Returns a new {@code InlineWidgetSize} larger or smaller by the given delta.
     * The delta is applied in the same unit as the current size.
     *
     * @param delta the amount to add (can be negative)
     * @return a new {@code InlineWidgetSize} instance
     */
    public InlineWidgetSize add(final float delta) {
        return new InlineWidgetSize(this.value + delta, this.unit);
    }

    @Override
    public String getCSSCode() {
        return this.pixels + "px";
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%.2f%s", this.value, this.unit);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof InlineWidgetSize)) return false;
        final InlineWidgetSize other = (InlineWidgetSize) obj;
        return Float.compare(this.value, other.value) == 0 && this.unit.equals(other.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value, this.unit);
    }

    @Override
    public int compareTo(final InlineWidgetSize other) {
        return Integer.compare(this.pixels, other.pixels);
    }
}
