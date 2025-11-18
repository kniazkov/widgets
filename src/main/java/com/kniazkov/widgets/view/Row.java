/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.protocol.AppendChild;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a table row widget that contains {@link Cell} widgets.
 */
public class Row extends Widget implements TypedContainer<Cell>,
        HasBgColor
{
    /**
     * Returns the default style instance used by table rows.
     *
     * @return the singleton default {@link RowStyle} instance
     */
    public static RowStyle getDefaultStyle() {
        return RowStyle.DEFAULT;
    }

    /**
     * List of child widgets.
     */
    private final List<Cell> children = new ArrayList<>();

    /**
     * Constructor.
     */
    public Row() {
        super(getDefaultStyle());
    }

    @Override
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public Cell getChild(int index) throws IndexOutOfBoundsException {
        return this.children.get(index);
    }

    @Override
    public void add(Cell widget) {
        this.children.add(widget);
        widget.setParent(this);
        pushUpdate(new AppendChild(widget.getId(), this.getId()));
    }

    @Override
    public void remove(Widget widget) {
        if (this.children.remove(widget)) {
            widget.setParent(null);
        }
    }

    @Override
    public String getType() {
        return "row";
    }

    /**
     * Sets a new widget style.
     *
     * @param style new widget style
     */
    public void setStyle(final RowStyle style) {
        super.setStyle(style);
    }
}
