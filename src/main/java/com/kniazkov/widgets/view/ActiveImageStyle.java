/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

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
