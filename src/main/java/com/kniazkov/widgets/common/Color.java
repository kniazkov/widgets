/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import com.kniazkov.json.JsonObject;

/**
 * Immutable RGB color representation.
 * This class encapsulates a color using three integer components: red, green, and blue,
 * each in the range [0, 255]. Values outside this range are clamped automatically.
 */
public final class Color {
    public static final Color BLACK = new Color(0, 0, 0);
    public static final Color WHITE = new Color(255, 255, 255);
    public static final Color RED = new Color(255, 0, 0);
    public static final Color GREEN = new Color(0, 127, 0);
    public static final Color BLUE = new Color(0, 0, 255);
    public static final Color DARK_SLATE_GRAY = new Color(47, 79, 79);

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
     * Constructs a new color with the specified RGB components.
     * Any component outside the range [0, 255] is clamped automatically.
     *
     * @param red red component
     * @param green green component
     * @param blue blue component
     */
    public Color(final int red, final int green, final int blue) {
        this.red = fixColor(red);
        this.green = fixColor(green);
        this.blue = fixColor(blue);
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
        return obj;
    }

    /**
     * Returns a string representation of the color in CSS-style {@code rgb(r,g,b)} format.
     *
     * @return human-readable string representation
     */
    @Override
    public String toString() {
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
            return this.red == other.red && this.green == other.green && this.blue == other.blue;
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
        int result = red;
        result = 31 * result + green;
        result = 31 * result + blue;
        return result;
    }

    /**
     * Ensures that a color component is within the valid range [0, 255].
     *
     * @param value the input component value
     * @return clamped value
     */
    private static int fixColor(final int value) {
        if (value < 0) return 0;
        if (value > 255) return 255;
        return value;
    }
}
