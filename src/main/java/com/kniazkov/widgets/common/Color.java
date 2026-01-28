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
     * Packs the color components (alpha, red, green, blue) into a single 32-bit integer
     * in ARGB format.
     * The resulting integer has the structure: {@code 0xAARRGGBB}.
     *
     * @return the packed ARGB value as a 32-bit integer
     */
    public int pack() {
        return (this.alpha << 24) | (this.red << 16) | (this.green << 8) | this.blue;
    }

    /**
     * Parses a color from various string representations.
     * Supported formats:
     * <ul>
     *   <li>RGB format: {@code "rgb(r,g,b)"} where r,g,b are integers in range [0,255]
     *       Example: {@code "rgb(255,0,127)"}</li>
     *   <li>RGBA format: {@code "rgba(r,g,b,a)"} where r,g,b are integers in range [0,255]
     *       and a is a floating-point number in range [0.0,1.0] or percentage string
     *       Examples: {@code "rgba(255,0,127,0.5)"}, {@code "rgba(255,0,127,50%)"}</li>
     *   <li>Color name: case-insensitive name from the predefined constants
     *       Examples: {@code "red"}, {@code "DARK_GRAY"}, {@code "LightGray"}</li>
     * </ul>
     * If the string cannot be parsed or is null, returns {@link Color#BLACK}.
     *
     * @param colorString the string to parse, may be null
     * @return parsed Color instance, or {@link Color#BLACK} if parsing fails
     */
    public static Color fromString(final String colorString) {
        if (colorString == null || colorString.trim().isEmpty()) {
            return Color.BLACK;
        }
        final String trimmed = colorString.trim();
        if (trimmed.startsWith("rgb")) {
            try {
                return parseRgbFormat(trimmed);
            } catch (final Exception ignored) {
                return Color.BLACK;
            }
        }
        final Color namedColor = parseColorName(trimmed);
        if (namedColor != null) {
            return namedColor;
        }
        return Color.BLACK;
    }

    /**
     * Parses RGB or RGBA format string.
     *
     * @param rgbString RGB/RGBA format string
     * @return parsed Color
     * @throws IllegalArgumentException if format is invalid
     */
    private static Color parseRgbFormat(final String rgbString) {
        final String normalized = rgbString.replace(" ", "").toLowerCase();
        if (normalized.startsWith("rgb(") && normalized.endsWith(")")) {
            final String content = normalized.substring(4, normalized.length() - 1);
            final String[] parts = content.split(",");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid RGB format");
            }
            final int r = Integer.parseInt(parts[0]);
            final int g = Integer.parseInt(parts[1]);
            final int b = Integer.parseInt(parts[2]);
            return new Color(r, g, b);
        } else if (normalized.startsWith("rgba(") && normalized.endsWith(")")) {
            final String content = normalized.substring(5, normalized.length() - 1);
            final String[] parts = content.split(",");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Invalid RGBA format");
            }
            final int r = Integer.parseInt(parts[0]);
            final int g = Integer.parseInt(parts[1]);
            final int b = Integer.parseInt(parts[2]);
            final double a = parseAlpha(parts[3]);
            final int alphaInt = (int) Math.round(a * 255);
            return new Color(r, g, b, alphaInt);
        }
        throw new IllegalArgumentException("Invalid RGB/RGBA format");
    }

    /**
     * Parses alpha channel value which can be a decimal number or percentage.
     *
     * @param alphaString alpha value as string (e.g., "0.5" or "50%")
     * @return alpha value in range [0.0,1.0]
     */
    private static double parseAlpha(final String alphaString) {
        if (alphaString.endsWith("%")) {
            final String percent = alphaString.substring(0, alphaString.length() - 1);
            final double value = Double.parseDouble(percent) / 100.0;
            return Math.max(0.0, Math.min(1.0, value));
        } else {
            final double value = Double.parseDouble(alphaString);
            return Math.max(0.0, Math.min(1.0, value));
        }
    }

    /**
     * Looks up a color by its name (case-insensitive).
     * Supports both constant names and common variations.
     *
     * @param colorName color name
     * @return Color instance or null if not found
     */
    private static Color parseColorName(final String colorName) {
        final String key = colorName.toUpperCase().replace("_", "");

        switch (key) {
            case "TRANSPARENT": return Color.TRANSPARENT;
            case "BLACK": return Color.BLACK;
            case "WHITE": return Color.WHITE;
            case "DARKGRAY":
            case "DARK_GRAY":
            case "DARKGREY":
            case "DARK_GREY": return Color.DARK_GRAY;
            case "GRAY":
            case "GREY": return Color.GRAY;
            case "LIGHTGRAY":
            case "LIGHT_GRAY":
            case "LIGHTGREY":
            case "LIGHT_GREY": return Color.LIGHT_GRAY;
            case "DARKSLATEGRAY":
            case "DARK_SLATE_GRAY":
            case "DARKSLATEGREY":
            case "DARK_SLATE_GREY": return Color.DARK_SLATE_GRAY;
            case "RED": return Color.RED;
            case "ORANGE": return Color.ORANGE;
            case "YELLOW": return Color.YELLOW;
            case "GREEN": return Color.GREEN;
            case "CYAN": return Color.CYAN;
            case "NAVY": return Color.NAVY;
            case "BLUE": return Color.BLUE;
            case "INDIGO": return Color.INDIGO;
            case "VIOLET": return Color.VIOLET;
            case "PINK": return Color.PINK;
            case "BROWN": return Color.BROWN;
            case "GOLD": return Color.GOLD;
            default: return null;
        }
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
