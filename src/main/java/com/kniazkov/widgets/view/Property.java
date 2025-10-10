/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Defines standard property keys used by widgets.
 * Each key represents a distinct bindable visual or behavioral property.
 */
public enum Property {
    /** Text content of the widget. */
    TEXT("text"),

    /** Foreground (text) color. */
    COLOR("color"),

    /** Background color. */
    BG_COLOR("bgColor"),

    /** Font face (typeface family). */
    FONT_FACE("fontFace"),

    /** Font size. */
    FONT_SIZE("fontSize"),

    /** Font weight (normal, bold, etc.). */
    FONT_WEIGHT("fontWeight"),

    /** Whether the font is italic. */
    ITALIC("italic"),

    /** Width of the widget. */
    WIDTH("width"),

    /** Height of the widget. */
    HEIGHT("height");

    private final String key;

    Property(final String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return this.key;
    }
}
