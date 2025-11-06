/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;

/**
 * Style definition for {@link TextWidget}.
 */
public class TextWidgetStyle extends Style implements HasStyledText, HasColor {
    /**
     * The global default text widget style.
     */
    public static final TextWidgetStyle DEFAULT = new TextWidgetStyle();

    /**
     * Creates the default text style.
     */
    private TextWidgetStyle() {
        this.setFontFace(FontFace.DEFAULT);
        this.setFontSize(FontSize.DEFAULT);
        this.setFontWeight(FontWeight.NORMAL);
        this.setItalic(false);
        this.setColor(Color.BLACK);
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
    public TextWidgetStyle derive() {
        return new TextWidgetStyle(this);
    }
}
