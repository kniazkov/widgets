/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.Color;

/**
 * Style definition for {@link Button}.
 */
public class ButtonStyle extends Style implements
        HasBgColor, HasHoverBgColor, HasFocusBgColor, HasDisabledBgColor,
        HasBorder, HasHoverBorder, HasFocusBorder, HasDisabledBorder,
        HasAbsoluteWidth, HasAbsoluteHeight, HasMargin, HasPadding
{
    /**
     * The global default button style.
     */
    public static final ButtonStyle DEFAULT = new ButtonStyle();

    /**
     * Creates the default button style.
     */
    private ButtonStyle() {
        this.setBgColor(new Color(200, 200, 200));
        this.setHoverBgColor(new Color(220, 220, 220));
        this.setFocusBgColor(Color.WHITE);
        this.setDisabledBgColor(Color.DARK_GRAY);

        this.setBorderColor(Color.DARK_GRAY);
        this.setHoverBorderColor(Color.BLACK);
        this.setFocusBorderColor(Color.ORANGE);
        this.setDisabledBorderColor(Color.LIGHT_GRAY);

        this.setBorderStyle(BorderStyle.SOLID);
        this.setBorderWidth("1px");
        this.setBorderRadius("3px");

        this.setWidth(AbsoluteSize.UNDEFINED);
        this.setHeight(AbsoluteSize.UNDEFINED);
        this.setMargin("2px");
        this.setPadding("10px");
    }

    /**
     * Creates a new button style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public ButtonStyle(final ButtonStyle parent) {
        super(parent);
    }

    @Override
    public ButtonStyle derive() {
        return new ButtonStyle(this);
    }
}
