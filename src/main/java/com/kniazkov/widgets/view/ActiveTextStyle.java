/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Cursor;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.TextDecoration;
import java.util.Set;

/**
 * Style definition for {@link ActiveText}.
 */
public class ActiveTextStyle extends Style implements HasStyledText, HasColor, HasCursor,
        HasTransition {
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(
        State.NORMAL,
        State.HOVERED,
        State.ACTIVE
    );

    /**
     * The global default active text widget style.
     */
    public static final ActiveTextStyle DEFAULT = new ActiveTextStyle();

    /**
     * Creates the default active text style.
     */
    private ActiveTextStyle() {
        this.setCursor(Cursor.POINTER);
        this.setTransition(DefaultTheme.TRANSITION);
        this.setFontFace(DefaultTheme.FONT);
        this.setFontSize("15px");
        this.setFontWeight(FontWeight.SEMIBOLD);
        this.setItalic(false);
        this.setTextDecoration(TextDecoration.NONE);
        this.setColor(State.NORMAL, DefaultTheme.PRIMARY);
        this.setColor(State.HOVERED, DefaultTheme.PRIMARY_HOVER);
        this.setColor(State.ACTIVE, DefaultTheme.PRIMARY_ACTIVE);
    }

    /**
     * Creates a new active text style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public ActiveTextStyle(final ActiveTextStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public ActiveTextStyle derive() {
        return new ActiveTextStyle(this);
    }
}
