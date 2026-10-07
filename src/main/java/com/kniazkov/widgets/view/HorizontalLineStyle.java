/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import java.util.Set;

/**
 * Style for a horizontal separator. Absolute height is the line thickness.
 */
public class HorizontalLineStyle extends Style
        implements HasColor, HasAbsoluteHeight, HasWidth, HasMargin, HasOpacity {
    /**
     * Supported visual states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(State.NORMAL);

    /**
     * Shared default separator style.
     */
    public static final HorizontalLineStyle DEFAULT = new HorizontalLineStyle();

    /**
     * Creates the default separator style.
     */
    private HorizontalLineStyle() {
        this.setColor(DefaultTheme.BORDER);
        this.setHeight(1);
        this.setWidth("100%");
        this.setMargin(0);
        this.setVerticalMargin(8);
        this.setOpacity(1.0);
    }

    /**
     * Creates a style inheriting from its parent.
     *
     * @param parent parent style
     */
    public HorizontalLineStyle(final HorizontalLineStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public HorizontalLineStyle derive() {
        return new HorizontalLineStyle(this);
    }
}
