/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.BoxShadow;
import com.kniazkov.widgets.common.BoxSizing;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Overflow;
import com.kniazkov.widgets.common.Transition;
import java.util.Set;

/**
 * Style definition for {@link Table}.
 */
public class TableStyle extends Style implements HasBgColor, HasBorder, HasWidth, HasHeight,
        HasMargin, HasPadding, HasCellSpacing, HasBoxShadow, HasTransition, HasBoxSizing,
        HasOverflow {
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(State.NORMAL);

    /**
     * The global default table style.
     */
    public static final TableStyle DEFAULT = new TableStyle();

    /**
     * Ready-to-use style for tables that display structured data.
     */
    public static final TableStyle DECORATED = createDecoratedStyle();

    /**
     * Style applied to rows created through the table API.
     */
    private final RowStyle rowStyle;

    /**
     * Style applied to cells created through the table API.
     */
    private final CellStyle cellStyle;

    /**
     * Creates the default table style.
     */
    private TableStyle() {
        this.setBoxShadow(BoxShadow.NONE);
        this.setTransition(Transition.NONE);
        this.setBoxSizing(BoxSizing.CONTENT_BOX);
        this.setOverflow(Overflow.VISIBLE);
        this.setBgColor(Color.TRANSPARENT);
        this.setBorderStyle(BorderStyle.NONE);
        this.setWidth(AbsoluteSize.UNDEFINED);
        this.setHeight(AbsoluteSize.UNDEFINED);
        this.setMargin(0);
        this.setPadding(0);
        this.setCellSpacing(0);
        this.rowStyle = Row.getDefaultStyle();
        this.cellStyle = Cell.getDefaultStyle();
    }

    /**
     * Creates a new table style that inherits models from the specified parent.
     *
     * @param parent the parent style to inherit from
     */
    public TableStyle(final TableStyle parent) {
        super(parent);
        this.rowStyle = parent.rowStyle.derive();
        this.cellStyle = parent.cellStyle.derive();
    }

    /**
     * Returns the style used for rows created through the table API.
     *
     * @return default row style associated with this table style
     */
    public RowStyle getDefaultRowStyle() {
        return this.rowStyle;
    }

    /**
     * Returns the style used for cells created through the table API.
     *
     * @return default cell style associated with this table style
     */
    public CellStyle getDefaultCellStyle() {
        return this.cellStyle;
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    @Override
    public TableStyle derive() {
        return new TableStyle(this);
    }

    /**
     * Creates the standard decorated data-table style.
     *
     * @return decorated table style
     */
    private static TableStyle createDecoratedStyle() {
        final TableStyle style = DEFAULT.derive();
        style.setBoxShadow(new BoxShadow(0, 2, 8, new Color(15, 23, 42, 24)));
        style.setTransition(DefaultTheme.TRANSITION);
        style.setBoxSizing(BoxSizing.BORDER_BOX);
        style.setOverflow(Overflow.HIDDEN);
        style.setBgColor(DefaultTheme.BORDER);
        style.setBorderColor(DefaultTheme.BORDER_STRONG);
        style.setBorderStyle(BorderStyle.SOLID);
        style.setBorderWidth(1);
        style.setBorderRadius(8);
        style.setWidth("100%");
        style.setMargin(2, 1);
        style.setCellSpacing(1);

        style.rowStyle.setBgColor(State.NORMAL, Color.WHITE);
        style.rowStyle.setBgColor(State.HOVERED, DefaultTheme.SURFACE_BLUE);
        style.rowStyle.setBgColor(State.ACTIVE, new Color(219, 234, 254));
        style.rowStyle.setTransition(DefaultTheme.TRANSITION);

        style.cellStyle.setPadding(14, 11);
        style.cellStyle.setBoxSizing(BoxSizing.BORDER_BOX);
        return style;
    }
}
