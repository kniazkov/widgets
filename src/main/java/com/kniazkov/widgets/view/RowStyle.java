/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Cursor;
import java.util.Set;

/**
 * Style definition for {@link Row}.
 */
public class RowStyle extends Style implements HasBgColor, HasCursor, HasTransition {
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(
        State.NORMAL,
        State.HOVERED,
        State.ACTIVE
    );

    /**
     * The global default row style.
     */
    public static final RowStyle DEFAULT = new RowStyle();

    /**
     * Creates the default row style.
     */
    private RowStyle() {
        this.setCursor(Cursor.AUTO);
        this.setTransition(DefaultTheme.TRANSITION);
        this.setBgColor(State.NORMAL, Color.WHITE);
        this.setBgColor(State.HOVERED, DefaultTheme.SURFACE_BLUE);
        this.setBgColor(State.ACTIVE, new Color(219, 234, 254));
    }

    /**
     * Creates a new row style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public RowStyle(final RowStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public RowStyle derive() {
        return new RowStyle(this);
    }
}
