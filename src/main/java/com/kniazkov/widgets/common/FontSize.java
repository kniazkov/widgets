package com.kniazkov.widgets.common;

import java.util.Locale;
import java.util.Objects;

/**
 * Represents a CSS-compatible font size using absolute units.
 * <p>
 * Internally stores both the original numeric value and unit (as specified or parsed),
 * along with a precomputed pixel equivalent. This allows operations such as creating
 * relative sizes (e.g., "one unit larger") while still providing pixel precision for the client.
 * <p>
 * Supported units:
 * <ul>
 *     <li>{@code px} — pixels</li>
 *     <li>{@code pt} — points (1pt = 1/72 inch)</li>
 *     <li>{@code pc} — picas (1pc = 12pt)</li>
 *     <li>{@code in} — inches</li>
 *     <li>{@code cm} — centimeters</li>
 *     <li>{@code mm} — millimeters</li>
 * </ul>
 * If no unit is provided when parsing a string, {@code px} is assumed.
 * The resolved pixel value is always clamped to a minimum of 3.
 */
public final class FontSize implements Comparable<FontSize> {
    /**
     * Default font size used when none is explicitly specified.
     * Equivalent to {@code 12pt} or {@code 16px}.
     */
    public static final FontSize DEFAULT = new FontSize("12pt");

    /**
     * Original numeric value provided or parsed (e.g. 12 from "12pt").
     */
    private final float value;

    /**
     * Original unit string (e.g. "pt", "px", etc.).
     */
    private final String unit;

    /**
     * Cached pixel equivalent.
     */
    private final int pixels;

    /**
     * Constructs a font size from a CSS-style string such as "12pt", "1in", or "16px".
     * If no unit is specified, pixels are assumed.
     *
     * @param str font size string to parse
     * @throws IllegalArgumentException if the input is invalid or contains unsupported units
     */
    public FontSize(final String str) {
        String s = str.trim().toLowerCase();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Empty font size");
        }

        int unitStart = 0;
        while (unitStart < s.length() &&
                (Character.isDigit(s.charAt(unitStart)) || s.charAt(unitStart) == '.')) {
            unitStart++;
        }

        if (unitStart == 0) {
            throw new IllegalArgumentException("No numeric part in font size: " + s);
        }

        this.value = Float.parseFloat(s.substring(0, unitStart));
        String parsedUnit = s.substring(unitStart).trim();
        if (parsedUnit.isEmpty()) {
            parsedUnit = "px";
        }

        this.unit = parsedUnit;
        this.pixels = clamp(toPixels(this.value, this.unit));
    }

    /**
     * Constructs a font size directly from a numeric value and unit string.
     *
     * @param value numeric font size (e.g. 12)
     * @param unit  unit string (e.g. "pt", "px", "cm", etc.)
     * @throws IllegalArgumentException if the unit is unsupported
     */
    public FontSize(final float value, final String unit) {
        if (unit == null || unit.isEmpty()) {
            throw new IllegalArgumentException("Unit must not be null or empty");
        }
        this.value = value;
        this.unit = unit.toLowerCase();
        this.pixels = clamp(toPixels(this.value, this.unit));
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
    private static int clamp(final int px) {
        return Math.max(3, px);
    }

    /**
     * Returns the numeric portion of the font size as originally specified.
     *
     * @return the numeric value (e.g. 12)
     */
    public float getValue() {
        return this.value;
    }

    /**
     * Returns the unit portion of the font size as originally specified.
     *
     * @return the unit string (e.g. "pt")
     */
    public String getUnit() {
        return this.unit;
    }

    /**
     * Returns the computed size in pixels.
     *
     * @return pixel value (minimum 3)
     */
    public int getPixels() {
        return this.pixels;
    }

    /**
     * Returns a new {@code FontSize} object that is larger by the specified delta.
     * The change is applied in the same unit as the current size.
     *
     * @param delta the amount to increase or decrease (e.g. +1 or -2)
     * @return a new {@code FontSize} instance with adjusted value
     */
    public FontSize add(final float delta) {
        return new FontSize(this.value + delta, this.unit);
    }

    /**
     * Returns the size as a CSS-compatible string, preserving the original unit.
     *
     * @return CSS size string (e.g., {@code "12pt"})
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%.2f%s", this.value, this.unit);
    }

    /**
     * Returns the size formatted in pixel units for client rendering.
     *
     * @return CSS size string in pixels (e.g., {@code "16px"})
     */
    public String getCSSCode() {
        return this.pixels + "px";
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof FontSize)) return false;
        final FontSize other = (FontSize) obj;
        return Float.compare(this.value, other.value) == 0 && this.unit.equals(other.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value, this.unit);
    }

    @Override
    public int compareTo(final FontSize other) {
        return Integer.compare(this.pixels, other.pixels);
    }
}
