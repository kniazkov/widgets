/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.BoxShadow;
import com.kniazkov.widgets.common.BoxSizing;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Cursor;
import com.kniazkov.widgets.common.Outline;
import java.util.Set;

/**
 * Style definition for {@link Button}.
 */
public class ButtonStyle extends Style implements HasBgColor, HasBorder, HasAbsoluteWidth,
        HasAbsoluteHeight, HasMargin, HasPadding, HasHiddenState, HasBoxShadow, HasOutline,
        HasCursor, HasTransition, HasBoxSizing {
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(
        State.NORMAL,
        State.HOVERED,
        State.FOCUSED,
        State.ACTIVE,
        State.DISABLED
    );

    /**
     * The global default button style.
     */
    public static final ButtonStyle DEFAULT = new ButtonStyle();

    /**
     * Creates the default button style.
     */
    private ButtonStyle() {
        this.setBoxShadow(BoxShadow.NONE);
        this.setBoxShadow(State.HOVERED,
            new BoxShadow(0, 3, 8, new Color(15, 23, 42, 32)));
        this.setBoxShadow(State.FOCUSED, DefaultTheme.FOCUS_SHADOW);
        this.setOutline(Outline.NONE);
        this.setOutline(State.FOCUSED, DefaultTheme.FOCUS_OUTLINE);
        this.setCursor(Cursor.POINTER);
        this.setCursor(State.DISABLED, Cursor.NOT_ALLOWED);
        this.setTransition(DefaultTheme.TRANSITION);
        this.setBoxSizing(BoxSizing.BORDER_BOX);
        this.setBgColor(State.NORMAL, Color.WHITE);
        this.setBgColor(State.HOVERED, DefaultTheme.SURFACE_MUTED);
        this.setBgColor(State.FOCUSED, Color.WHITE);
        this.setBgColor(State.ACTIVE, DefaultTheme.SURFACE_DISABLED);
        this.setBgColor(State.DISABLED, DefaultTheme.SURFACE_MUTED);

        this.setBorderColor(State.NORMAL, DefaultTheme.BORDER_STRONG);
        this.setBorderColor(State.HOVERED, DefaultTheme.TEXT);
        this.setBorderColor(State.FOCUSED, DefaultTheme.PRIMARY);
        this.setBorderColor(State.ACTIVE, DefaultTheme.BORDER_STRONG);
        this.setBorderColor(State.DISABLED, DefaultTheme.BORDER);

        this.setBorderStyle(BorderStyle.SOLID);

        this.setBorderWidth(1);
        this.setBorderRadius(8);

        this.setWidth(AbsoluteSize.UNDEFINED);
        this.setHeight(40);
        this.setMargin(2);
        this.setPadding(16, 8);
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
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public ButtonStyle derive() {
        return new ButtonStyle(this);
    }
}
