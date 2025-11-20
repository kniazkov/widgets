/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.Color;
import java.util.Set;

/**
 * Style definition for {@link Cell}.
 */
public class CellStyle extends Style implements HasBgColor, HasBorder, HasWidth, HasHeight
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
     * The global default cell style.
     */
    public static final CellStyle DEFAULT = new CellStyle();

    /**
     * Creates the default cell style.
     */
    private CellStyle() {
        this.setBgColor(Color.WHITE);
        this.setBorderStyle(BorderStyle.NONE);
        this.setWidth(AbsoluteSize.UNDEFINED);
        this.setHeight(AbsoluteSize.UNDEFINED);
    }

    /**
     * Creates a new cell style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public CellStyle(final CellStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public CellStyle derive() {
        return new CellStyle(this);
    }
}
