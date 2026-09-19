/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import com.kniazkov.json.JsonObject;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Immutable RGB color representation.
 * This class encapsulates a color using three integer components: red, green, blue, and alpha
 * each in the range [0, 255]. Values outside this range are clamped automatically.
 */
public final class Color {
    /**
     * Fully transparent (0, 0, 0, 0).
     */
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    /**
     * Black (0, 0, 0).
     */
    public static final Color BLACK = new Color(0, 0, 0);

    /**
     * White (255, 255, 255).
     */
    public static final Color WHITE = new Color(255, 255, 255);

    /**
     * Dark gray (64, 64, 64).
     */
    public static final Color DARK_GRAY = new Color(64, 64, 64);

    /**
     * Gray (128, 128, 128).
     */
    public static final Color GRAY = new Color(128, 128, 128);

    /**
     * Light gray (192, 192, 192).
     */
    public static final Color LIGHT_GRAY = new Color(192, 192, 192);

    /**
     * Dark slate gray (47, 79, 79).
     */
    public static final Color DARK_SLATE_GRAY = new Color(47, 79, 79);

    /**
     * Red (255, 0, 0).
     */
    public static final Color RED = new Color(255, 0, 0);

    /**
     * Orange (255, 165, 0).
     */
    public static final Color ORANGE = new Color(255, 165, 0);

    /**
     * Yellow (255, 255, 0).
     */
    public static final Color YELLOW = new Color(255, 255, 0);

    /**
     * Green (0, 127, 0).
     */
    public static final Color GREEN = new Color(0, 127, 0);

    /**
     * Cyan (0, 255, 255).
     */
    public static final Color CYAN = new Color(0, 255, 255);

    /**
     * Navy (0, 0, 127).
     */
    public static final Color NAVY = new Color(0, 0, 127);

    /**
     * Blue (0, 0, 255).
     */
    public static final Color BLUE = new Color(0, 0, 255);

    /**
     * Indigo (75, 0, 130).
     */
    public static final Color INDIGO = new Color(75, 0, 130);

    /**
     * Violet (148, 0, 211).
     */
    public static final Color VIOLET = new Color(148, 0, 211);

    /**
     * Pink (255, 192, 203).
     */
    public static final Color PINK = new Color(255, 192, 203);

    /**
     * Brown (165, 42, 42).
     */
    public static final Color BROWN = new Color(165, 42, 42);

    /**
     * Gold (255, 215, 0).
     */
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
        return (double) Math.round((double) this.alpha / 2.55) / 100;
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
     * CSS decimal number, excluding Java-only NaN, Infinity and hexadecimal floats.
     */
    private static final Pattern NUMBER = Pattern.compile(
        "[+-]?(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?"
    );

    /**
     * Parses HEX (#RGB, #RGBA, #RRGGBB, #RRGGBBAA), RGB/RGBA, HSL/HSLA,
     * or a predefined color name. Functions accept comma syntax or space-separated
     * channels with optional slash alpha. RGB channels accept numbers or percentages;
     * HSL saturation/lightness require percentages. Hue accepts degrees (also unitless),
     * radians, gradians or turns. Alpha accepts a number or percentage.
     * Names/functions are case-insensitive. Components are clamped and hue wraps.
     * HEX alpha is last, unlike {@link #pack()}. Invalid or null input returns BLACK.
     * This is a concrete color parser, not a full CSS expression evaluator.
     *
     * @param colorString string to parse, may be null
     * @return parsed color, or BLACK on invalid input
     */
    public static Color fromString(final String colorString) {
        if (colorString == null) {
            return BLACK;
        }
        final String value = colorString.trim().toLowerCase(Locale.ROOT);
        try {
            if (value.startsWith("#")) {
                return parseHex(value.substring(1));
            }
            final int open = value.indexOf('(');
            if (open >= 0 && value.endsWith(")")) {
                final String function = value.substring(0, open);
                if (function.equals("rgb") || function.equals("rgba")
                    || function.equals("hsl") || function.equals("hsla")) {
                    return parseFunction(function, value.substring(open + 1, value.length() - 1));
                }
            }
            final Color named = parseColorName(value);
            return named == null ? BLACK : named;
        } catch (final IllegalArgumentException ignored) {
            return BLACK;
        }
    }

    /**
     * Parses CSS HEX with alpha in the last component.
     */
    private static Color parseHex(final String hex) {
        if (!hex.matches("(?:[0-9a-f]{3}|[0-9a-f]{4}|[0-9a-f]{6}|[0-9a-f]{8})")) {
            throw new IllegalArgumentException("Invalid HEX color");
        }
        final int step = hex.length() <= 4 ? 1 : 2;
        final int[] channels = {0, 0, 0, 255};
        for (int index = 0; index < hex.length() / step; index++) {
            channels[index] = Integer.parseInt(hex.substring(index * step, (index + 1) * step), 16)
                * (step == 1 ? 17 : 1);
        }
        return new Color(channels[0], channels[1], channels[2], channels[3]);
    }

    /**
     * Parses the two supported function separator styles without mixing them.
     */
    private static Color parseFunction(final String function, final String body) {
        final boolean comma = body.contains(",");
        final String[] channels;
        final String alpha;
        if (comma) {
            final String[] parts = body.split(",", -1);
            if (body.contains("/") || (parts.length != 3 && parts.length != 4)) {
                throw new IllegalArgumentException("Invalid comma syntax");
            }
            channels = new String[] {parts[0].trim(), parts[1].trim(), parts[2].trim()};
            alpha = parts.length == 4 ? parts[3].trim() : "1";
        } else {
            final String[] parts = body.split("/", -1);
            if (parts.length > 2) {
                throw new IllegalArgumentException("Invalid slash syntax");
            }
            channels = parts[0].trim().split("\\s+");
            alpha = parts.length == 2 ? parts[1].trim() : "1";
        }
        if (channels.length != 3) {
            throw new IllegalArgumentException("Expected three channels");
        }
        final int opacity = byteValue(component(alpha, 1));
        if (function.startsWith("hsl")) {
            return fromHsl(hue(channels[0]), percentage(channels[1]),
                percentage(channels[2]), opacity);
        }
        if (comma && (channels[0].endsWith("%") != channels[1].endsWith("%")
            || channels[0].endsWith("%") != channels[2].endsWith("%"))) {
            throw new IllegalArgumentException("Mixed legacy RGB units");
        }
        return new Color(byteValue(component(channels[0], 255)),
            byteValue(component(channels[1], 255)),
            byteValue(component(channels[2], 255)), opacity);
    }

    /**
     * Parses a finite CSS number.
     */
    private static double number(final String token) {
        if (!NUMBER.matcher(token).matches()) {
            throw new IllegalArgumentException("Invalid number");
        }
        final double value = Double.parseDouble(token);
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Non-finite number");
        }
        return value;
    }

    /**
     * Normalizes a numeric or percentage component to [0, 1].
     */
    private static double component(final String token, final double scale) {
        final double value = token.endsWith("%")
            ? number(token.substring(0, token.length() - 1)) / 100 : number(token) / scale;
        return Math.max(0, Math.min(1, value));
    }

    /**
     * Parses a required percentage.
     */
    private static double percentage(final String token) {
        if (!token.endsWith("%")) {
            throw new IllegalArgumentException("Expected percentage");
        }
        return component(token, 1);
    }

    /**
     * Rounds a normalized channel to an eight-bit value.
     */
    private static int byteValue(final double value) {
        return (int) Math.round(value * 255);
    }

    /**
     * Converts and wraps a hue to [0, 360), reducing before multiplying to avoid overflow.
     */
    private static double hue(final String token) {
        final String[] units = {"deg", "grad", "rad", "turn"};
        final double[] periods = {360, 400, 2 * Math.PI, 1};
        for (int index = 0; index < units.length; index++) {
            if (token.endsWith(units[index])) {
                final double value = number(
                    token.substring(0, token.length() - units[index].length())
                );
                return ((value % periods[index]) / periods[index] * 360 + 360) % 360;
            }
        }
        return (number(token) % 360 + 360) % 360;
    }

    /**
     * Converts HSL to sRGB using chroma and the hue sector.
     */
    private static Color fromHsl(final double hue, final double saturation,
        final double lightness, final int alpha) {
        final double chroma = (1 - Math.abs(2 * lightness - 1)) * saturation;
        final double x = chroma * (1 - Math.abs((hue / 60) % 2 - 1));
        final double offset = lightness - chroma / 2;
        final double[][] sectors = {
            {chroma, x, 0}, {x, chroma, 0}, {0, chroma, x},
            {0, x, chroma}, {x, 0, chroma}, {chroma, 0, x}
        };
        final double[] rgb = sectors[(int) (hue / 60)];
        return new Color(byteValue(rgb[0] + offset), byteValue(rgb[1] + offset),
            byteValue(rgb[2] + offset), alpha);
    }

    /**
     * Looks up a color by its name (case-insensitive).
     * Supports both constant names and common variations.
     *
     * @param colorName color name
     * @return Color instance or null if not found
     */
    private static Color parseColorName(final String colorName) {
        final String key = colorName.toUpperCase(Locale.ROOT).replace("_", "");

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
        if (value < 0) {
            return 0;
        }
        if (value > 255) {
            return 255;
        }
        return value;
    }
}
