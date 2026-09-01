/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.BoxShadow;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Cursor;
import java.util.Set;

/**
 * Style definition for {@link ActiveImage}.
 */
public class ActiveImageStyle extends ImageWidgetStyle {
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(
        State.NORMAL,
        State.HOVERED,
        State.ACTIVE
    );

    /**
     * The global default image widget style.
     */
    public static final ActiveImageStyle DEFAULT = new ActiveImageStyle();

    /**
     * Creates the default image style.
     */
    private ActiveImageStyle() {
        this.setCursor(Cursor.POINTER);
        this.setOpacity(State.NORMAL, 1.0);
        this.setOpacity(State.HOVERED, 0.94);
        this.setOpacity(State.ACTIVE, 0.84);
        this.setBoxShadow(State.NORMAL,
            new BoxShadow(0, 4, 12, new Color(15, 23, 42, 38)));
        this.setBoxShadow(State.HOVERED,
            new BoxShadow(0, 8, 20, new Color(15, 23, 42, 56)));
        this.setBoxShadow(State.ACTIVE,
            new BoxShadow(0, 2, 6, new Color(15, 23, 42, 45)));
    }

    /**
     * Creates a new image style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public ActiveImageStyle(final ActiveImageStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public ActiveImageStyle derive() {
        return new ActiveImageStyle(this);
    }
}
