/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import java.util.Locale;
import java.util.Objects;

/**
 * Represents an absolute, CSS-compatible font size.
 * <p>
 * Stores both the original numeric value and unit (e.g., {@code "12pt"}) along
 * with a precomputed pixel equivalent. A minimum pixel size of 3px is enforced.
 * <p>
 * Supported units:
 * <ul>
 *     <li>{@code px} — pixels</li>
 *     <li>{@code pt} — points (1pt = 1/72 inch)</li>
 *     <li>{@code pc} — picas (1pc = 12pt = 16px)</li>
 *     <li>{@code in} — inches</li>
 *     <li>{@code cm} — centimeters</li>
 *     <li>{@code mm} — millimeters</li>
 * </ul>
 */
public final class FontSize implements Comparable<FontSize> {
    /**
     * Default font size = 12pt (≈ 16px).
     */
    public static final FontSize DEFAULT = new FontSize(12f, Unit.PT);

    /**
     * Original numeric value.
     */
    private final float value;

    /**
     * Original unit.
     */
    private final Unit unit;

    /**
     * Precomputed pixel value (minimum 3).
     */
    private final int pixels;

    /**
     * Parses a CSS-style font size: "12px", "1.2in", "15pt".
     * If unit is omitted, "px" is assumed.
     *
     * @param str input string
     * @throws IllegalArgumentException if invalid format
     */
    public static FontSize parse(final String str) {
        if (str == null) {
            throw new IllegalArgumentException("Font size string is null");
        }
        final String s = str.trim().toLowerCase();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Empty font size");
        }

        int i = 0;
        while (i < s.length() &&
            (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
            i++;
        }

        if (i == 0) {
            throw new IllegalArgumentException("Invalid font size format: " + str);
        }

        final float value = Float.parseFloat(s.substring(0, i));
        final Unit unit = Unit.fromString(s.substring(i).trim());

        return new FontSize(value, unit);
    }

    /**
     * Creates a font size.
     *
     * @param value value ≥ 0
     * @param unit  absolute unit
     */
    public FontSize(final float value, final Unit unit) {
        if (value < 0f) {
            throw new IllegalArgumentException("Font size value must be >= 0");
        }
        this.value = value;
        this.unit = Objects.requireNonNull(unit, "Unit must not be null");
        this.pixels = clamp(toPixels(value, unit));
    }

    /**
     * Converts an absolute unit value to pixels.
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
     * Minimum allowed pixel value for legibility.
     */
    private static int clamp(final int px) {
        return Math.max(3, px);
    }

    /** Returns the original numeric value. */
    public float getValue() {
        return this.value;
    }

    /** Returns the original unit. */
    public Unit getUnit() {
        return this.unit;
    }

    /** Returns the computed pixel value (≥ 3). */
    public int getPixels() {
        return this.pixels;
    }

    /**
     * Returns a new FontSize with modified value, preserving unit.
     */
    public FontSize add(final float delta) {
        final float v = this.value + delta;
        if (v < 0f) {
            return new FontSize(0f, this.unit);
        }
        return new FontSize(v, this.unit);
    }

    /**
     * Returns CSS code in original units.
     */
    @Override
    public String toString() {
        // Two decimals only if needed
        if (value == (long) value) {
            return String.format(Locale.ROOT, "%d%s", (long) value, unit);
        }
        return String.format(Locale.ROOT, "%.2f%s", value, unit);
    }

    /**
     * Returns CSS code in pixel units.
     */
    public String getCSSCode() {
        return this.pixels + "px";
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof FontSize)) return false;
        final FontSize other = (FontSize) obj;
        return Float.compare(this.value, other.value) == 0 && this.unit == other.unit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, unit);
    }

    /**
     * Order by pixel size.
     */
    @Override
    public int compareTo(final FontSize other) {
        return Integer.compare(this.pixels, other.pixels);
    }
}
