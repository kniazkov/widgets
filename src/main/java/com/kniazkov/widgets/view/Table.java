/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.protocol.AppendChild;
import com.kniazkov.widgets.protocol.RemoveChild;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a table widget that contains {@link Row} widgets.
 */
public class Table extends BlockWidget implements TypedContainer<Row>,
        HasBgColor, HasBorder, HasWidth, HasHeight, HasMargin, HasPadding, HasCellSpacing
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
     * Cache of column views, indexed by column index.
     * Uses TreeMap to maintain columns in sorted order by their index.
     */
    private final Map<Integer, Column> columns = new TreeMap<>();

    /**
     * The default style applied to new rows created by this table.
     */
    private RowStyle rowStyle = Row.getDefaultStyle();

    /**
     * The default style applied to new cells created by this table.
     */
    private CellStyle cellStyle = Cell.getDefaultStyle();

    /**
     * Constructs a new Table with the default style.
     */
    public Table() {
        super(getDefaultStyle());
    }

    /**
     * Constructs a new Table with the specified style.
     *
     * @param style the table style to use
     */
    public Table(final TableStyle style) {
        super(style);
    }

    @Override
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public Row getChild(final int index) throws IndexOutOfBoundsException {
        return this.children.get(index);
    }

    @Override
    public void add(final Row widget) {
        this.children.add(widget);
        widget.setParent(this);
        pushUpdate(new AppendChild(widget.getId(), this.getId()));
    }

    @Override
    public void remove(final Widget widget) {
        if (this.children.remove(widget)) {
            this.pushUpdate(new RemoveChild(widget.getId(), this.getId()));
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
     * Sets the default style for new rows created in this table.
     * This style will be applied to all rows created after this method is called.
     *
     * @param style the row style to set as default
     */
    public void setDefaultRowStyle(final RowStyle style) {
        this.rowStyle = style;
    }

    /**
     * Sets the default style for new cells created in this table.
     * This style will be applied to all cells created after this method is called.
     *
     * @param style the cell style to set as default
     */
    public void setDefaultCellStyle(final CellStyle style) {
        this.cellStyle = style;
    }

    /**
     * Returns the default cell style for the specified column.
     * If a custom style is defined for the column, returns that style; otherwise returns the
     * table's default cell style.
     *
     * @param columnIndex the index of the column to get the style for
     * @return the default cell style for the specified column
     */
    public CellStyle getDefaultCellStyle(final int columnIndex) {
        final Column column = this.columns.get(columnIndex);
        if (column == null) {
            return this.cellStyle;
        } else {
            return column.getDefaultCellStyle();
        }
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
            row = new Row(this.rowStyle);
            this.add(row);
        } while (index >= this.children.size());
        return row;
    }

    /**
     * Returns a logical view of the column at the specified index.
     * <p>
     * Columns do not exist as standalone widgets in the hierarchy; instead, this method creates
     * a lightweight {@link Column} wrapper that provides convenient access to the cells of the
     * requested column.
     *
     * @param index the zero-based column index (must be >= 0)
     * @return a {@link Column} view representing the specified column
     * @throws IndexOutOfBoundsException if {@code index} is negative
     */
    public Column getColumn(final int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Column index must be >= 0");
        }
        return this.columns.computeIfAbsent(index, x -> new Column(this, index));
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
