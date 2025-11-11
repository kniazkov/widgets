/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.BooleanModel;
import com.kniazkov.widgets.model.FontFaceModel;
import com.kniazkov.widgets.model.FontSizeModel;
import com.kniazkov.widgets.model.FontWeightModel;

/**
 * Style definition for {@link InputField}.
 */
public class InputFieldStyle extends Style implements
        HasStyledText, HasHoverStyledText, HasFocusStyledText, HasDisabledStyledText, HasInvalidStyledText,
        HasColor, HasHoverColor, HasFocusColor, HasDisabledColor, HasInvalidColor,
        HasBgColor, HasHoverBgColor, HasFocusBgColor, HasDisabledBgColor, HasInvalidBgColor,
        HasBorder, HasHoverBorder, HasFocusBorder, HasDisabledBorder, HasInvalidBorder,
        HasAbsoluteWidth, HasAbsoluteHeight, HasMargin
{
    /**
     * The global default input field style.
     */
    public static final InputFieldStyle DEFAULT = new InputFieldStyle();

    /**
     * Creates the default input field style.
     */
    private InputFieldStyle() {
        final FontFaceModel face = new FontFaceModel(FontFace.DEFAULT);
        this.setFontFaceModel(face);
        this.setHoverFontFaceModel(face.asCascading());
        this.setFocusFontFaceModel(face.asCascading());
        this.setInvalidFontFaceModel(face.asCascading());
        this.setDisabledFontFaceModel(face.asCascading());

        final FontSizeModel size = new FontSizeModel(FontSize.DEFAULT);
        this.setFontSizeModel(size);
        this.setHoverFontSizeModel(size.asCascading());
        this.setFocusFontSizeModel(size.asCascading());
        this.setInvalidFontSizeModel(size.asCascading());
        this.setDisabledFontSizeModel(size.asCascading());

        final FontWeightModel weight = new FontWeightModel(FontWeight.NORMAL);
        this.setFontWeightModel(weight);
        this.setHoverFontWeightModel(weight.asCascading());
        this.setFocusFontWeightModel(weight.asCascading());
        this.setInvalidFontWeightModel(weight.asCascading());
        this.setDisabledFontWeightModel(weight.asCascading());

        final BooleanModel italic = new BooleanModel(false);
        this.setItalicModel(italic);
        this.setHoverItalicModel(italic.asCascading());
        this.setFocusItalicModel(italic.asCascading());
        this.setInvalidItalicModel(italic.asCascading());
        this.setDisabledItalicModel(italic.asCascading());

        this.setColor(Color.BLACK);
        this.setHoverColor(Color.BLUE);
        this.setFocusColor(Color.ORANGE);
        this.setInvalidColor(Color.RED);
        this.setDisabledColor(Color.DARK_GRAY);

        this.setBgColor(Color.WHITE);
        this.setHoverBgColor(Color.YELLOW);
        this.setFocusBgColor(Color.CYAN);
        this.setInvalidBgColor(Color.PINK);
        this.setDisabledBgColor(Color.LIGHT_GRAY);

        this.setBorderColor(Color.DARK_GRAY);
        this.setHoverBorderColor(Color.BLACK);
        this.setFocusBorderColor(Color.ORANGE);
        this.setInvalidBorderColor(Color.RED);
        this.setDisabledBorderColor(Color.LIGHT_GRAY);

        this.setBorderStyle(BorderStyle.SOLID);
        this.setBorderWidth("1px");
        this.setBorderRadius("0px");

        this.setWidth("100px");
        this.setHeight("25px");
        this.setMargin("2px", "1px");
    }

    /**
     * Creates a new input field style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public InputFieldStyle(final InputFieldStyle parent) {
        super(parent);
    }

    @Override
    public InputFieldStyle derive() {
        return new InputFieldStyle(this);
    }
}
