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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Style definition for {@link InputField}.
 */
public class InputFieldStyle extends Style implements
        HasStyledText, HasHoverStyledText, HasFocusStyledText, HasDisabledStyledText, HasInvalidStyledText,
        HasColor,
        HasBgColor,
        HasBorder,
        HasAbsoluteWidth, HasAbsoluteHeight, HasMargin
{
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            State.NORMAL,
            State.HOVERED,
            State.ACTIVE,
            State.DISABLED,
            State.INVALID
        )));

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

        this.setColor(State.NORMAL, Color.BLACK);
        this.setColor(State.HOVERED, Color.BLUE);
        this.setColor(State.ACTIVE, Color.ORANGE);
        this.setColor(State.INVALID, Color.RED);
        this.setColor(State.DISABLED, Color.DARK_GRAY);

        this.setBgColor(State.NORMAL, Color.WHITE);
        this.setBgColor(State.HOVERED, Color.YELLOW);
        this.setBgColor(State.ACTIVE, Color.CYAN);
        this.setBgColor(State.INVALID, Color.PINK);
        this.setBgColor(State.DISABLED, Color.LIGHT_GRAY);

        this.setBorderColor(State.NORMAL, Color.DARK_GRAY);
        this.setBorderColor(State.HOVERED, Color.BLACK);
        this.setBorderColor(State.ACTIVE, Color.ORANGE);
        this.setBorderColor(State.INVALID, Color.RED);
        this.setBorderColor(State.DISABLED, Color.LIGHT_GRAY);

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
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public InputFieldStyle derive() {
        return new InputFieldStyle(this);
    }
}
