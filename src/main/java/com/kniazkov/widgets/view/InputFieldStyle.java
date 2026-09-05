/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.BoxShadow;
import com.kniazkov.widgets.common.BoxSizing;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Cursor;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.HorizontalAlignment;
import com.kniazkov.widgets.common.Outline;
import com.kniazkov.widgets.common.TextDecoration;
import com.kniazkov.widgets.model.BooleanModel;
import com.kniazkov.widgets.model.FontFaceModel;
import com.kniazkov.widgets.model.FontSizeModel;
import com.kniazkov.widgets.model.FontWeightModel;
import com.kniazkov.widgets.model.TextDecorationModel;
import java.util.Set;

/**
 * Style definition for {@link InputField}.
 */
public class InputFieldStyle extends Style implements HasStyledText, HasColor, HasBgColor,
        HasBorder, HasAbsoluteWidth, HasAbsoluteHeight, HasMargin, HasPadding, HasBoxShadow,
        HasOutline, HasCursor, HasTransition, HasBoxSizing, HasHorizontalAlignment {
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(
        State.NORMAL,
        State.HOVERED,
        State.FOCUSED,
        State.ACTIVE,
        State.DISABLED,
        State.INVALID
    );

    /**
     * The global default input field style.
     */
    public static final InputFieldStyle DEFAULT = new InputFieldStyle();

    /**
     * Creates the default input field style.
     */
    private InputFieldStyle() {
        this.setBoxShadow(BoxShadow.NONE);
        this.setBoxShadow(State.FOCUSED, DefaultTheme.FOCUS_SHADOW);
        this.setOutline(Outline.NONE);
        this.setOutline(State.FOCUSED, DefaultTheme.FOCUS_OUTLINE);
        this.setCursor(Cursor.TEXT);
        this.setCursor(State.DISABLED, Cursor.NOT_ALLOWED);
        this.setTransition(DefaultTheme.TRANSITION);
        this.setBoxSizing(BoxSizing.BORDER_BOX);
        final FontFaceModel face = new FontFaceModel(DefaultTheme.FONT);
        this.setFontFaceModel(State.NORMAL, face);
        this.setFontFaceModel(State.HOVERED, face.asCascading());
        this.setFontFaceModel(State.FOCUSED, face.asCascading());
        this.setFontFaceModel(State.ACTIVE, face.asCascading());
        this.setFontFaceModel(State.INVALID, face.asCascading());
        this.setFontFaceModel(State.DISABLED, face.asCascading());

        final FontSizeModel size = new FontSizeModel("15px");
        this.setFontSizeModel(State.NORMAL, size);
        this.setFontSizeModel(State.HOVERED, size.asCascading());
        this.setFontSizeModel(State.FOCUSED, size.asCascading());
        this.setFontSizeModel(State.ACTIVE, size.asCascading());
        this.setFontSizeModel(State.INVALID, size.asCascading());
        this.setFontSizeModel(State.DISABLED, size.asCascading());

        final FontWeightModel weight = new FontWeightModel(FontWeight.NORMAL);
        this.setFontWeightModel(State.NORMAL, weight);
        this.setFontWeightModel(State.HOVERED, weight.asCascading());
        this.setFontWeightModel(State.FOCUSED, weight.asCascading());
        this.setFontWeightModel(State.ACTIVE, weight.asCascading());
        this.setFontWeightModel(State.INVALID, weight.asCascading());
        this.setFontWeightModel(State.DISABLED, weight.asCascading());

        final BooleanModel italic = new BooleanModel(false);
        this.setItalicModel(State.NORMAL, italic);
        this.setItalicModel(State.HOVERED, italic.asCascading());
        this.setItalicModel(State.FOCUSED, italic.asCascading());
        this.setItalicModel(State.ACTIVE, italic.asCascading());
        this.setItalicModel(State.INVALID, italic.asCascading());
        this.setItalicModel(State.DISABLED, italic.asCascading());

        final TextDecorationModel decoration = new TextDecorationModel(TextDecoration.NONE);
        this.setTextDecorationModel(State.NORMAL, decoration);
        this.setTextDecorationModel(State.HOVERED, decoration.asCascading());
        this.setTextDecorationModel(State.FOCUSED, decoration.asCascading());
        this.setTextDecorationModel(State.ACTIVE, decoration.asCascading());
        this.setTextDecorationModel(State.INVALID, decoration.asCascading());
        this.setTextDecorationModel(State.DISABLED, decoration.asCascading());

        this.setColor(State.NORMAL, DefaultTheme.TEXT);
        this.setColor(State.HOVERED, DefaultTheme.TEXT);
        this.setColor(State.FOCUSED, DefaultTheme.TEXT);
        this.setColor(State.ACTIVE, DefaultTheme.TEXT);
        this.setColor(State.INVALID, DefaultTheme.DANGER);
        this.setColor(State.DISABLED, DefaultTheme.MUTED);

        this.setBgColor(State.NORMAL, Color.WHITE);
        this.setBgColor(State.HOVERED, DefaultTheme.SURFACE_MUTED);
        this.setBgColor(State.FOCUSED, Color.WHITE);
        this.setBgColor(State.ACTIVE, Color.WHITE);
        this.setBgColor(State.INVALID, DefaultTheme.SURFACE_RED);
        this.setBgColor(State.DISABLED, DefaultTheme.SURFACE_DISABLED);

        this.setBorderColor(State.NORMAL, DefaultTheme.BORDER_STRONG);
        this.setBorderColor(State.HOVERED, DefaultTheme.TEXT);
        this.setBorderColor(State.FOCUSED, DefaultTheme.PRIMARY);
        this.setBorderColor(State.ACTIVE, DefaultTheme.PRIMARY);
        this.setBorderColor(State.INVALID, DefaultTheme.DANGER);
        this.setBorderColor(State.DISABLED, DefaultTheme.BORDER);

        this.setBorderStyle(BorderStyle.SOLID);

        this.setBorderWidth(1);
        this.setBorderRadius(8);

        this.setWidth(240);
        this.setHeight(42);
        this.setMargin(2, 1);
        this.setPadding(8);
        this.setHorizontalAlignment(HorizontalAlignment.LEFT);
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
