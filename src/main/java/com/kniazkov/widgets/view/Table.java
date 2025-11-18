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
        HasBgColor, HasBorder
{
    /**
     * Returns the default style instance used by tables.
     *
     * @return the singleton default {@link TableStyle} instance
     */
    public static TableStyle getDefaultStyle() {
        return TableStyle.DEFAULT;
    }

    /**
     * List of child widgets.
     */
    private final List<Row> children = new ArrayList<>();

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

    /**
     * Returns the row at the specified index.
     * <p>
     * If the index is within the current list size, the existing row is returned.
     * If the index is greater than or equal to the number of rows, missing rows are automatically
     * created, appended to this table, and the newly created row at the requested index
     * is returned.
     *
     * @param index the row index (must be >= 0)
     * @return the existing or newly created cell at the given index
     * @throws IndexOutOfBoundsException if {@code index} is negative
     */
    public Row getRow(final int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Row index must be >= 0");
        }
        if (index < this.children.size()) {
            return this.children.get(index);
        }
        Row row;
        do {
            row = new Row();
            this.add(row);
        } while (index >= this.children.size());
        return row;
    }

    /**
     * Returns the cell at the specified row and column.
     * <p>
     * If the row or the cell does not exist yet, missing rows and/or cells are automatically
     * created. This allows dynamic construction of tables without needing to pre-allocate rows
     * and columns manually.
     *
     * @param row the row index (must be >= 0)
     * @param column the column index (must be >= 0)
     * @return the existing or newly created cell at the given coordinates
     * @throws IndexOutOfBoundsException if {@code row} is negative or {@code column} is negative
     */
    public Cell getCell(int row, int column) {
        return this.getRow(row).getCell(column);
    }
}
