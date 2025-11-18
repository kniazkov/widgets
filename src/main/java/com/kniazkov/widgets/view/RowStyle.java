/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import java.util.Set;

/**
 * Style definition for {@link Row}.
 */
public class RowStyle extends Style implements HasBgColor
{
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
        this.setBgColor(Color.WHITE);
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
