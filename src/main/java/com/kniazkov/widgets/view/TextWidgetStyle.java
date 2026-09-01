/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.FontWeight;
import java.util.Set;

/**
 * Style definition for {@link TextWidget}.
 */
public class TextWidgetStyle extends Style implements HasStyledText, HasColor {
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(State.NORMAL);

    /**
     * The global default text widget style.
     */
    public static final TextWidgetStyle DEFAULT = new TextWidgetStyle();

    /**
     * Creates the default text style.
     */
    private TextWidgetStyle() {
        this.setFontFace(DefaultTheme.FONT);
        this.setFontSize("15px");
        this.setFontWeight(FontWeight.NORMAL);
        this.setItalic(false);
        this.setColor(DefaultTheme.TEXT);
    }

    /**
     * Creates a new text style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public TextWidgetStyle(final TextWidgetStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public TextWidgetStyle derive() {
        return new TextWidgetStyle(this);
    }
}
