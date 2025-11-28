/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.HandlesPointerEvents;
import com.kniazkov.widgets.protocol.AppendChild;
import com.kniazkov.widgets.protocol.RemoveChild;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a table row widget that contains {@link Cell} widgets.
 */
public class Row extends Widget implements TypedContainer<Cell>,
        HasBgColor, HandlesPointerEvents
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
     * Creates a new row using the default row style.
     */
    public Row() {
        super(getDefaultStyle());
    }

    /**
     * Creates a new row with the specified style.
     *
     * @param style the style to apply to this row
     */
    public Row(final RowStyle style) {
        super(style);
    }

    @Override
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public Cell getChild(final int index) throws IndexOutOfBoundsException {
        return this.children.get(index);
    }

    @Override
    public void add(final Cell widget) {
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

    /**
     * Returns the cell at the specified index.
     * <p>
     * If the index is within the current list size, the existing cell is returned.
     * If the index is greater than or equal to the number of cells, missing cells are automatically
     * created, appended to this row, and the newly created cell at the requested index is returned.
     *
     * @param index the cell index (must be >= 0)
     * @return the existing or newly created cell at the given index
     * @throws IndexOutOfBoundsException if {@code index} is negative
     */
    public Cell getCell(final int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Cell index must be >= 0");
        }
        if (index < this.children.size()) {
            return this.children.get(index);
        }
        final Optional<Container> parent = this.getParent();
        if (parent.isPresent() && parent.get() instanceof Table) {
            final Table table = (Table) parent.get();
            Cell cell;
            int currIndex;
            do {
                currIndex = this.children.size();
                cell = new Cell(table.getDefaultCellStyle(currIndex));
                this.add(cell);
            } while (index > currIndex);
            return cell;
        } else {
            Cell cell;
            do {
                cell = new Cell();
                this.add(cell);
            } while (index >= this.children.size());
            return cell;
        }
    }
}
