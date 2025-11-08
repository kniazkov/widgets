/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.BooleanModel;
import com.kniazkov.widgets.model.FontFaceModel;
import com.kniazkov.widgets.model.FontSizeModel;
import com.kniazkov.widgets.model.FontWeightModel;

/**
 * Style definition for {@link Button}.
 */
public class ButtonStyle extends Style implements
        HasBgColor, HasHoverBgColor, HasFocusBgColor, HasDisabledBgColor,
        HasAbsoluteWidth, HasAbsoluteHeight
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

        this.setWidth(AbsoluteSize.UNDEFINED);
        this.setHeight("25px");
    }

    /**
     * Creates a new input field style that inherits models from the specified parent.
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
