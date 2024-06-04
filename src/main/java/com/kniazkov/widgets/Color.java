/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;

/**
 * Class representing a color.
 */
public final class Color {
    /**
     * Black color.
     */
    public static final Color BLACK = new Color(0,0,0);

    /**
     * Red color.
     */
    public static final Color RED = new Color(255,0,0);

    /**
     * Green color.
     */
    public static final Color GREEN = new Color(0,255,0);

    /**
     * Blue color.
     */
    public static final Color BLUE = new Color(0,0,255);

    /**
     * Red component.
     */
    private final int red;

    /**
     * Green component.
     */
    private final int green;

    /**
     * Blue component.
     */
    private final int blue;

    /**
     * Constructor.
     * @param red Red component
     * @param green Green component
     * @param blue Blue component
     */
    public Color(final int red, final int green, final int blue) {
        this.red = fixColor(red);
        this.green = fixColor(green);
        this.blue = fixColor(blue);
    }

    /**
     * Transforms the color into an object suitable for use on a client.
     * @return JSON object containing color components
     */
    public JsonObject toJsonObject() {
        JsonObject obj = new JsonObject();
        obj.addNumber("r", this.red);
        obj.addNumber("g", this.green);
        obj.addNumber("b", this.blue);
        return obj;
    }

    /**
     * Returns a string representation of the color.
     * @return String representation of the color
     */
    @Override
    public String toString() {
        return "rgb(" + this.red + ',' + this.green + ',' + this.blue + ')';
    }

    /**
     * Compares color to other object
     * @param obj Another object
     * @return Comparison result ({@code true} if other object is a color and has
     *  the same component values)
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
     * Checks the value of the color component. If it is incorrect, fixes it.
     * @param value Value of the color component
     * @return Fixed value
     */
    private static int fixColor(final int value) {
        if (value < 0) return  0;
        if (value > 255) return 255;
        return value;
    }
}
