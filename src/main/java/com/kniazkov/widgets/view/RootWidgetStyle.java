/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import java.util.Collections;
import java.util.Set;

/**
 * Style definition for {@link RootWidget}.
 */
public class RootWidgetStyle extends Style implements HasBgColor
{
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = Collections.singleton(State.NORMAL);

    /**
     * The global default root widget style.
     */
    public static final RootWidgetStyle DEFAULT = new RootWidgetStyle();

    /**
     * Creates the default root widget style.
     */
    private RootWidgetStyle() {
        this.setBgColor(Color.WHITE);
    }

    /**
     * Creates a new root widget style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public RootWidgetStyle(final RootWidgetStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public RootWidgetStyle derive() {
        return new RootWidgetStyle(this);
    }
}
