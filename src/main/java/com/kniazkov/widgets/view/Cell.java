/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.HandlesPointerEvents;
import com.kniazkov.widgets.protocol.AppendChild;
import com.kniazkov.widgets.protocol.RemoveChild;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a table cell widget capable of containing {@link BlockWidget}s.
 */
public class Cell extends Widget<CellStyle> implements BlockContainer,
        HasBgColor, HasBorder, HasWidth, HasHeight, HasPadding, HasVerticalAlignment,
        HandlesPointerEvents
{
    /**
     * Returns the default style instance used by table cells.
     *
     * @return the singleton default {@link CellStyle} instance
     */
    public static CellStyle getDefaultStyle() {
        return CellStyle.DEFAULT;
    }

    /**
     * List of child widgets.
     */
    private final List<BlockWidget<?>> children = new ArrayList<>();

    /**
     * Constructs a new cell with the default style.
     */
    public Cell() {
        super(getDefaultStyle());
    }

    /**
     * Constructs a new cell with the specified style.
     *
     * @param style the cell style to use
     */
    public Cell(final CellStyle style) {
        super(style);
    }

    @Override
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public BlockWidget<?> getChild(final int index) throws IndexOutOfBoundsException {
        return this.children.get(index);
    }

    @Override
    public void add(final BlockWidget<?> widget) {
        this.children.add(widget);
        widget.setParent(this);
        pushUpdate(new AppendChild(widget.getId(), this.getId()));
    }

    @Override
    public void remove(final Widget<?> widget) {
        if (this.children.remove(widget)) {
            this.pushUpdate(new RemoveChild(widget.getId(), this.getId()));
            widget.setParent(null);
        }
    }

    @Override
    public String getType() {
        return "cell";
    }
}
