/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A container for default styles used across the application.
 * <p>
 *     This class provides centralized access to reusable style definitions (e.g., for text widgets,
 *     buttons, etc.), allowing consistent styling and easy style reuse or customization.
 * </p>
 */
public final class StyleSet {

    /**
     * Default style for {@link TextWidget} instances.
     */
    private final TextWidgetStyle textWidgetStyle;

    /**
     * Default style for {@link InputField} instances.
     */
    private final InputFieldStyle inputFieldStyle;

    /**
     * Constructs a style set with predefined defaults.
     */
    StyleSet() {
        this.textWidgetStyle = new TextWidgetStyle();
        this.inputFieldStyle = new InputFieldStyle();
    }

    /**
     * Returns the default style for {@link TextWidget}s.
     *
     * @return Shared text widget style
     */
    public TextWidgetStyle getDefaultTextWidgetStyle() {
        return this.textWidgetStyle;
    }

    /**
     * Returns the default style for {@link InputField}s.
     *
     * @return Shared input field style
     */
    public InputFieldStyle getDefaultInputFieldStyle() {
        return this.inputFieldStyle;
    }
}
