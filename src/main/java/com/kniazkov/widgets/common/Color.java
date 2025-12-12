/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import com.kniazkov.json.JsonObject;

/**
 * Immutable RGB color representation.
 * This class encapsulates a color using three integer components: red, green, blue, and alpha
 * each in the range [0, 255]. Values outside this range are clamped automatically.
 */
public final class Color {
    /** Fully transparent (0, 0, 0, 0). */
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    /** Black (0, 0, 0). */
    public static final Color BLACK = new Color(0, 0, 0);

    /** White (255, 255, 255). */
    public static final Color WHITE = new Color(255, 255, 255);

    /** Dark gray (64, 64, 64). */
    public static final Color DARK_GRAY = new Color(64, 64, 64);

    /** Gray (128, 128, 128). */
    public static final Color GRAY = new Color(128, 128, 128);

    /** Light gray (192, 192, 192). */
    public static final Color LIGHT_GRAY = new Color(192, 192, 192);

    /** Dark slate gray (47, 79, 79). */
    public static final Color DARK_SLATE_GRAY = new Color(47, 79, 79);

    /** Red (255, 0, 0). */
    public static final Color RED = new Color(255, 0, 0);

    /** Orange (255, 165, 0). */
    public static final Color ORANGE = new Color(255, 165, 0);

    /** Yellow (255, 255, 0). */
    public static final Color YELLOW = new Color(255, 255, 0);

    /** Green (0, 127, 0). */
    public static final Color GREEN = new Color(0, 127, 0);

    /** Cyan (0, 255, 255). */
    public static final Color CYAN = new Color(0, 255, 255);

    /** Navy (0, 0, 127). */
    public static final Color NAVY = new Color(0, 0, 127);

    /** Blue (0, 0, 255). */
    public static final Color BLUE = new Color(0, 0, 255);

    /** Indigo (75, 0, 130). */
    public static final Color INDIGO = new Color(75, 0, 130);

    /** Violet (148, 0, 211). */
    public static final Color VIOLET = new Color(148, 0, 211);

    /** Pink (255, 192, 203). */
    public static final Color PINK = new Color(255, 192, 203);

    /** Brown (165, 42, 42). */
    public static final Color BROWN = new Color(165, 42, 42);

    /** Gold (255, 215, 0). */
    public static final Color GOLD = new Color(255, 215, 0);

    /**
     * Red component in range [0, 255].
     */
    private final int red;

    /**
     * Green component in range [0, 255].
     */
    private final int green;

    /**
     * Blue component in range [0, 255].
     */
    private final int blue;

    /**
     * Alpha channel in range [0, 255] (transparency, 0 = fully transparent).
     */
    private final int alpha;

    /**
     * Constructs a new color with the specified RGBA components.
     * Any component outside the range [0, 255] is clamped automatically.
     *
     * @param red red component
     * @param green green component
     * @param blue blue component
     * @param alpha alpha component
     */
    public Color(final int red, final int green, final int blue, final int alpha) {
        this.red = fix(red);
        this.green = fix(green);
        this.blue = fix(blue);
        this.alpha = fix(alpha);
    }

    /**
     * Constructs a new color with the specified RGB components.
     * Any component outside the range [0, 255] is clamped automatically.
     *
     * @param red red component
     * @param green green component
     * @param blue blue component
     */
    public Color(final int red, final int green, final int blue) {
        this.red = fix(red);
        this.green = fix(green);
        this.blue = fix(blue);
        this.alpha = 255;
    }

    /**
     * Converts this color to a JSON object with fields {@code "r"}, {@code "g"}, and {@code "b"}.
     *
     * @return a {@link JsonObject} representing this color
     */
    public JsonObject toJsonObject() {
        JsonObject obj = new JsonObject();
        obj.addNumber("r", this.red);
        obj.addNumber("g", this.green);
        obj.addNumber("b", this.blue);
        if (this.alpha < 255) {
            obj.addNumber("a", this.getAlphaAsPercent());
        }
        return obj;
    }

    /**
     * Returns a string representation of the color in CSS-style {@code rgb(r,g,b)} format.
     *
     * @return human-readable string representation
     */
    @Override
    public String toString() {
        if (this.alpha < 255) {
            return "rgba(" + this.red + ',' + this.green + ',' + this.blue + ','
                + this.getAlphaAsPercent() + ')';
        }
        return "rgb(" + this.red + ',' + this.green + ',' + this.blue + ')';
    }

    /**
     * Compares this color to another object.
     *
     * @param obj The object to compare
     * @return {@code true} if equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Color) {
            final Color other = (Color) obj;
            return this.red == other.red && this.green == other.green && this.blue == other.blue
                && this.alpha == other.alpha;
        }
        return false;
    }

    /**
     * Computes a hash code consistent with {@link #equals(Object)}.
     *
     * @return hash code of this color
     */
    @Override
    public int hashCode() {
        int result = this.red;
        result = 31 * result + this.green;
        result = 31 * result + this.blue;
        result = 31 * result + this.alpha;
        return result;
    }

    /**
     * Returns the red component of this color.
     *
     * @return red value in the range [0, 255]
     */
    public int getRed() {
        return this.red;
    }

    /**
     * Returns the green component of this color.
     *
     * @return green value in the range [0, 255]
     */
    public int getGreen() {
        return this.green;
    }

    /**
     * Returns the blue component of this color.
     *
     * @return blue value in the range [0, 255]
     */
    public int getBlue() {
        return this.blue;
    }

    /**
     * Returns the alpha (transparency) component of this color.
     *
     * @return alpha value in the range [0, 255], where 0 is fully transparent
     */
    public int getAlpha() {
        return this.alpha;
    }

    /**
     * Reads alpha channel value in the range [0, 1].
     * @return alpha channel value in the range [0, 1]
     */
    public double getAlphaAsPercent() {
        return (double)Math.round((double)this.alpha / 2.55) / 100;
    }

    /**
     * Ensures that a color component is within the valid range [0, 255].
     *
     * @param value the input component value
     * @return clamped value
     */
    private static int fix(final int value) {
        if (value < 0) return 0;
        if (value > 255) return 255;
        return value;
    }
}
