/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Represents standard font weight values used in web typography.
 * This enum corresponds to common CSS and Google Fonts weights, ranging from {@code 100} (thin)
 * to {@code 900} (black). These values can be used in styling engines, CSS generation,
 * or text rendering settings.
 */
public enum FontWeight {
    /**
     * Ultra-thin font weight. CSS value: 100.
     * Suitable for large text or decorative uses.
     */
    THIN(100),

    /**
     * Extra-light font weight. CSS value: 200.
     */
    EXTRA_LIGHT(200),

    /**
     * Light font weight. CSS value: 300.
     */
    LIGHT(300),

    /**
     * Normal or regular font weight. CSS value: 400.
     * This is the browser/user agent default.
     */
    NORMAL(400),

    /**
     * Medium font weight. CSS value: 500.
     */
    MEDIUM(500),

    /**
     * Semi-bold font weight. CSS value: 600.
     * Slightly heavier than normal, but lighter than bold.
     */
    SEMIBOLD(600),

    /**
     * Bold font weight. CSS value: 700.
     * Common for emphasis (e.g., headings, labels).
     */
    BOLD(700),

    /**
     * Extra-bold font weight. CSS value: 800.
     */
    EXTRA_BOLD(800),

    /**
     * Heavy/black font weight. CSS value: 900.
     * Strong visual impact; used sparingly.
     */
    BLACK(900);

    /**
     * Numeric representation of the font weight, compatible with CSS.
     */
    private final int weight;

    /**
     * Constructor.
     * @param weight weight numeric value
     */
    FontWeight(int weight) {
        this.weight = weight;
    }

    /**
     * Returns the numeric font weight value (e.g., 400 for normal, 700 for bold).
     *
     * @return CSS-compatible weight value
     */
    public int getWeight() {
        return this.weight;
    }

    /**
     * Returns a lowercase string version of the name, with underscores replaced by spaces.
     * Useful for UI display or serialization.
     *
     * @return human-readable name (e.g., "extra light")
     */
    @Override
    public String toString() {
        return this.name().toLowerCase().replace('_', ' ');
    }

    /**
     * Parses a numeric weight and maps it to the closest enum constant.
     * If the exact value is not defined, falls back to {@link #NORMAL}.
     *
     * @param weight the numeric weight value
     * @return corresponding {@code FontWeight} or {@code NORMAL} if unknown
     */
    public static FontWeight fromWeight(int weight) {
        for (final FontWeight fw : values()) {
            if (fw.weight == weight) return fw;
        }
        return NORMAL;
    }
}
