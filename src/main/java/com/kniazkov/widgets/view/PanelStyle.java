/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.Color;
import java.util.Set;

/**
 * Style definition for {@link Panel}.
 */
public class PanelStyle extends Style implements HasBgColor, HasBorder, HasWidth, HasHeight,
        HasMargin, HasPadding, HasBoxShadow, HasCursor, HasTransition, HasBoxSizing {
    /**
     * Supported visual states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(
        State.NORMAL,
        State.HOVERED,
        State.ACTIVE
    );

    /**
     * The global default panel style.
     */
    public static final PanelStyle DEFAULT = new PanelStyle();

    /**
     * Creates the default panel style.
     */
    private PanelStyle() {
        this.setBgColor(Color.TRANSPARENT);
        this.setBorderStyle(BorderStyle.NONE);
        this.setWidth(AbsoluteSize.UNDEFINED);
        this.setHeight(AbsoluteSize.UNDEFINED);
        this.setMargin(0);
        this.setPadding(0);
    }

    /**
     * Creates a new panel style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public PanelStyle(final PanelStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public PanelStyle derive() {
        return new PanelStyle(this);
    }
}
