/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.RMId;
import java.util.Objects;

/**
 * Represents a logical view of a table column.
 * <p>
 * Columns are not actual widgets and do not exist in the visual hierarchy.
 * This wrapper simply provides a convenient way to access and iterate over the cells of a specific
 * column inside a {@link Table}.
 */
public class Column implements TypedContainer<Cell> {
    /**
     * The parent table from which this column extracts its cells.
     */
    private final Table table;

    /**
     * The zero-based column index represented by this wrapper.
     */
    private final int number;

    /**
     * The default style applied to new cells created by this column.
     */
    private CellStyle cellStyle = Cell.getDefaultStyle();

    /**
     * Creates a new column wrapper for the given table and column index.
     *
     * @param table  the parent table
     * @param number the zero-based index of the column
     */
    Column(final Table table, final int number) {
        this.table = table;
        this.number = number;
    }

    @Override
    public RMId getId() {
        return RMId.INVALID;
    }

    @Override
    public int getChildCount() {
        return this.table.getChildCount();
    }

    @Override
    public Cell getChild(final int index) throws IndexOutOfBoundsException {
        return this.table.getRow(index).getCell(this.number);
    }

    @Override
    public void add(final Cell cell) {
        Objects.requireNonNull(cell, "cell").validateAttachment(this.table);
        final Row row = this.table.createDefaultRow();
        for (int index = 0; index < this.number; index++) {
            row.add(new Cell(this.table.getDefaultCellStyle(index)));
        }
        this.table.add(row);
        row.add(cell);
    }

    @Override
    public void remove(final Widget<?> widget) {
        for (int index = 0; index < this.table.getChildCount(); index++) {
            final Row row = this.table.getChild(index);
            if (this.number < row.getChildCount() && row.getChild(this.number) == widget) {
                row.remove(widget);
                return;
            }
        }
    }

    @Override
    public void removeAll() {
        for (int index = 0; index < this.table.getChildCount(); index++) {
            final Row row = this.table.getChild(index);
            if (this.number < row.getChildCount()) {
                row.remove(row.getChild(this.number));
            }
        }
    }

    /**
     * Sets the default style for new cells created in this column.
     * This style will be applied to all cells created after this method is called.
     *
     * @param style the cell style to set as default
     */
    public void setDefaultCellStyle(final CellStyle style) {
        this.cellStyle = style;
    }

    /**
     * Returns the default style used for new cells in this column.
     *
     * @return the current default cell style
     */
    public CellStyle getDefaultCellStyle() {
        return this.cellStyle;
    }
}
