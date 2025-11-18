/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.protocol.AppendChild;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a table cell widget capable of containing {@link BlockWidget}s.
 */
public final class Cell extends Widget implements TypedContainer<BlockWidget>,
        HasBgColor {
    public static CellStyle getDefaultStyle() {
        return CellStyle.DEFAULT;
    }

    /**
     * List of child widgets.
     */
    final List<BlockWidget> children = new ArrayList<>();

    /**
     * Constructor.
     */
    public Cell() {
        super(getDefaultStyle());
    }

    @Override
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public BlockWidget getChild(int index) throws IndexOutOfBoundsException {
        return this.children.get(index);
    }

    @Override
    public void add(BlockWidget widget) {
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
        return "cell";
    }

    /**
     * Sets a new widget style.
     *
     * @param style new widget style
     */
    public void setStyle(final CellStyle style) {
        super.setStyle(style);
    }
}
