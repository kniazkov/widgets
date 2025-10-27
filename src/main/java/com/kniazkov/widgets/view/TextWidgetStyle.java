/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * A style definition for {@link TextWidget} instances.
 * This class itself does not introduce new properties — it merely aggregates style aspects
 * provided by its mixin interfaces.
 */
public class TextWidgetStyle extends Style implements HasStyledText, HasColor {
    /**
     * Creates an empty text style with no predefined models.
     * This constructor is protected because styles are typically instantiated through
     * {@link #derive()} or by subclassing.
     */
    protected TextWidgetStyle() {
    }

    /**
     * Creates a new text style that inherits all reactive models from the specified parent style.
     *
     * @param parent the parent style to inherit from
     */
    public TextWidgetStyle(final TextWidgetStyle parent) {
        super(parent);
    }

    @Override
    public TextWidgetStyle derive() {
        return new TextWidgetStyle(this);
    }
}
