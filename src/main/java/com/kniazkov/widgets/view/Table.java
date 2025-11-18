/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.protocol.AppendChild;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a table widget that contains {@link Row} widgets.
 */
public class Table extends BlockWidget implements TypedContainer<Row>,
        HasBgColor, HasBorder {
    public static TableStyle getDefaultStyle() {
        return TableStyle.DEFAULT;
    }

    /**
     * List of child widgets.
     */
    final List<Row> children = new ArrayList<>();

    /**
     * Constructor.
     */
    public Table() {
        super(getDefaultStyle());
    }

    @Override
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public Row getChild(int index) throws IndexOutOfBoundsException {
        return this.children.get(index);
    }

    @Override
    public void add(Row widget) {
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
        return "table";
    }

    /**
     * Sets a new widget style.
     *
     * @param style new widget style
     */
    public void setStyle(final TableStyle style) {
        super.setStyle(style);
    }
}
