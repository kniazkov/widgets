/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;

import java.util.Set;

/**
 * Style definition for {@link CheckBox}.
 */
public class CheckBoxStyle extends Style implements HasColor, HasBgColor, HasSelectableImage {
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(
            State.NORMAL,
            State.HOVERED,
            State.ACTIVE,
            State.DISABLED
    );

    /**
     * The global default check box style.
     */
    public static final CheckBoxStyle DEFAULT = new CheckBoxStyle();

    /**
     * Creates the default text style.
     */
    private CheckBoxStyle() {
        this.setColor(Color.BLACK);
        this.setBgColor(Color.WHITE);
    }

    /**
     * Creates a new text style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public CheckBoxStyle(final CheckBoxStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public CheckBoxStyle derive() {
        return new CheckBoxStyle(this);
    }
}
