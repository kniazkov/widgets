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
public class InputFieldStyle extends Style implements HasStyledText, HasColor, HasBgColor,
        HasBorder, HasAbsoluteWidth, HasAbsoluteHeight, HasMargin
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
        this.setFontFaceModel(State.NORMAL, face);
        this.setFontFaceModel(State.HOVERED, face.asCascading());
        this.setFontFaceModel(State.ACTIVE, face.asCascading());
        this.setFontFaceModel(State.INVALID, face.asCascading());
        this.setFontFaceModel(State.DISABLED, face.asCascading());

        final FontSizeModel size = new FontSizeModel(FontSize.DEFAULT);
        this.setFontSizeModel(State.NORMAL, size);
        this.setFontSizeModel(State.HOVERED, size.asCascading());
        this.setFontSizeModel(State.ACTIVE, size.asCascading());
        this.setFontSizeModel(State.INVALID, size.asCascading());
        this.setFontSizeModel(State.DISABLED, size.asCascading());

        final FontWeightModel weight = new FontWeightModel(FontWeight.NORMAL);
        this.setFontWeightModel(State.NORMAL, weight);
        this.setFontWeightModel(State.HOVERED, weight.asCascading());
        this.setFontWeightModel(State.ACTIVE, weight.asCascading());
        this.setFontWeightModel(State.INVALID, weight.asCascading());
        this.setFontWeightModel(State.DISABLED, weight.asCascading());

        final BooleanModel italic = new BooleanModel(false);
        this.setItalicModel(State.NORMAL, italic);
        this.setItalicModel(State.HOVERED, italic.asCascading());
        this.setItalicModel(State.ACTIVE, italic.asCascading());
        this.setItalicModel(State.INVALID, italic.asCascading());
        this.setItalicModel(State.DISABLED, italic.asCascading());

        this.setColor(State.NORMAL, Color.BLACK);
        this.setColor(State.INVALID, Color.RED);
        this.setColor(State.DISABLED, Color.DARK_GRAY);

        this.setBgColor(State.NORMAL, Color.WHITE);
        this.setBgColor(State.HOVERED, new Color(255, 255, 230));
        this.setBgColor(State.ACTIVE, new Color(255, 255, 204));
        this.setBgColor(State.INVALID, new Color(255, 230, 230));
        this.setBgColor(State.DISABLED, Color.LIGHT_GRAY);

        this.setBorderColor(State.NORMAL, Color.GRAY);
        this.setBorderColor(State.HOVERED, Color.DARK_GRAY);
        this.setBorderColor(State.ACTIVE, Color.BLACK);
        this.setBorderColor(State.DISABLED, Color.DARK_GRAY);

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
