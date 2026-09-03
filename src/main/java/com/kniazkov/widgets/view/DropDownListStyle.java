/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.BoxShadow;
import com.kniazkov.widgets.common.BoxSizing;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Cursor;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.Outline;
import com.kniazkov.widgets.common.TextDecoration;
import com.kniazkov.widgets.model.BooleanModel;
import com.kniazkov.widgets.model.FontFaceModel;
import com.kniazkov.widgets.model.FontSizeModel;
import com.kniazkov.widgets.model.FontWeightModel;
import com.kniazkov.widgets.model.TextDecorationModel;
import java.util.Set;

/**
 * Style definition for {@link DropDownList}.
 */
public final class DropDownListStyle extends Style implements HasStyledText, HasColor, HasBgColor,
        HasBorder, HasAbsoluteWidth, HasAbsoluteHeight, HasMargin, HasPadding, HasBoxShadow,
        HasOutline, HasCursor, HasTransition, HasBoxSizing {
    /**
     * Supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(
        State.NORMAL, State.HOVERED, State.FOCUSED, State.ACTIVE, State.DISABLED
    );

    /**
     * Global default style.
     */
    public static final DropDownListStyle DEFAULT = new DropDownListStyle();

    /**
     * Creates the default style.
     */
    private DropDownListStyle() {
        this.setBoxShadow(BoxShadow.NONE);
        this.setBoxShadow(State.FOCUSED, DefaultTheme.FOCUS_SHADOW);
        this.setOutline(Outline.NONE);
        this.setOutline(State.FOCUSED, DefaultTheme.FOCUS_OUTLINE);
        this.setCursor(Cursor.POINTER);
        this.setCursor(State.DISABLED, Cursor.NOT_ALLOWED);
        this.setTransition(DefaultTheme.TRANSITION);
        this.setBoxSizing(BoxSizing.BORDER_BOX);

        final FontFaceModel face = new FontFaceModel(DefaultTheme.FONT);
        final FontSizeModel size = new FontSizeModel("15px");
        final FontWeightModel weight = new FontWeightModel(FontWeight.NORMAL);
        final BooleanModel italic = new BooleanModel(false);
        final TextDecorationModel decoration = new TextDecorationModel(TextDecoration.NONE);
        for (final State state : SUPPORTED_STATES) {
            this.setFontFaceModel(state, state == State.NORMAL ? face : face.asCascading());
            this.setFontSizeModel(state, state == State.NORMAL ? size : size.asCascading());
            this.setFontWeightModel(state, state == State.NORMAL ? weight : weight.asCascading());
            this.setItalicModel(state, state == State.NORMAL ? italic : italic.asCascading());
            this.setTextDecorationModel(
                state, state == State.NORMAL ? decoration : decoration.asCascading()
            );
        }

        this.setColor(State.NORMAL, DefaultTheme.TEXT);
        this.setColor(State.HOVERED, DefaultTheme.TEXT);
        this.setColor(State.FOCUSED, DefaultTheme.TEXT);
        this.setColor(State.ACTIVE, DefaultTheme.TEXT);
        this.setColor(State.DISABLED, DefaultTheme.MUTED);
        this.setBgColor(State.NORMAL, Color.WHITE);
        this.setBgColor(State.HOVERED, DefaultTheme.SURFACE_MUTED);
        this.setBgColor(State.FOCUSED, Color.WHITE);
        this.setBgColor(State.ACTIVE, Color.WHITE);
        this.setBgColor(State.DISABLED, DefaultTheme.SURFACE_DISABLED);
        this.setBorderColor(State.NORMAL, DefaultTheme.BORDER_STRONG);
        this.setBorderColor(State.HOVERED, DefaultTheme.TEXT);
        this.setBorderColor(State.FOCUSED, DefaultTheme.PRIMARY);
        this.setBorderColor(State.ACTIVE, DefaultTheme.PRIMARY);
        this.setBorderColor(State.DISABLED, DefaultTheme.BORDER);
        this.setBorderStyle(BorderStyle.SOLID);
        this.setBorderWidth(1);
        this.setBorderRadius(8);
        this.setWidth(240);
        this.setHeight(42);
        this.setMargin(2, 1);
        this.setPadding(8);
    }

    /**
     * Creates a derived style.
     *
     * @param parent parent style
     */
    public DropDownListStyle(final DropDownListStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public DropDownListStyle derive() {
        return new DropDownListStyle(this);
    }
}
