/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.BoxShadow;
import com.kniazkov.widgets.common.BoxSizing;
import com.kniazkov.widgets.common.Color;
import java.util.Set;

/**
 * Style definition for {@link Table}.
 */
public class TableStyle extends Style implements HasBgColor, HasBorder, HasWidth, HasHeight,
        HasMargin, HasPadding, HasCellSpacing, HasBoxShadow, HasTransition, HasBoxSizing {
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
        this.setBoxShadow(new BoxShadow(0, 2, 8, new Color(15, 23, 42, 24)));
        this.setTransition(DefaultTheme.TRANSITION);
        this.setBoxSizing(BoxSizing.BORDER_BOX);
        this.setBgColor(Color.WHITE);
        this.setBorderColor(DefaultTheme.BORDER_STRONG);
        this.setBorderStyle(BorderStyle.SOLID);
        this.setBorderWidth(1);
        this.setBorderRadius(8);
        this.setWidth("100%");
        this.setHeight(AbsoluteSize.UNDEFINED);
        this.setMargin(2, 1);
        this.setPadding(0);
        this.setCellSpacing(0);
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
