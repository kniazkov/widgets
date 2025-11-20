/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.Color;
import java.util.Set;

/**
 * Style definition for {@link Table}.
 */
public class TableStyle extends Style implements HasBgColor, HasBorder, HasWidth, HasHeight,
        HasMargin, HasPadding
{
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(State.NORMAL);

    /**
     * The global default table style.
     */
    public static final TableStyle DEFAULT = new TableStyle();

    /**
     * Creates the default table style.
     */
    private TableStyle() {
        this.setBgColor(Color.TRANSPARENT);
        this.setBorderStyle(BorderStyle.NONE);
        this.setWidth(AbsoluteSize.UNDEFINED);
        this.setHeight(AbsoluteSize.UNDEFINED);
        this.setMargin(0);
        this.setPadding(0);
    }

    /**
     * Creates a new table style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public TableStyle(final TableStyle parent) {
        super(parent);
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public TableStyle derive() {
        return new TableStyle(this);
    }
}
