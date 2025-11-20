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
public class Cell extends Widget implements TypedContainer<BlockWidget>,
        HasBgColor, HasBorder, HasWidth, HasHeight, HasPadding,
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
    private final List<BlockWidget> children = new ArrayList<>();

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
    public BlockWidget getChild(final int index) throws IndexOutOfBoundsException {
        return this.children.get(index);
    }

    @Override
    public void add(final BlockWidget widget) {
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

    /**
     * Replaces all content of this cell with a single text section.
     *
     * @param text the text to display inside the cell
     */
    public void setText(final String text) {
        this.removeAll();
        final Section section = new Section();
        this.add(section);
        section.add(new TextWidget(text));
    }
}
