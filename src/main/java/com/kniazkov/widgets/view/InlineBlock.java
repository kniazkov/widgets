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
 * A widget that behaves as an inline element in the flow of text but can contain
 * block-level widgets as children, similar to the CSS {@code display: inline-block} behavior.
 * <p>
 * This widget combines the characteristics of inline and block elements:
 * <ul>
 *   <li>It flows with surrounding text and other inline elements</li>
 *   <li>It can have width, height, padding, and other block-level properties</li>
 *   <li>It can contain block-level widgets as children</li>
 *   <li>It creates a block formatting context for its children</li>
 * </ul>
 * This makes it useful for creating complex inline elements that need to contain
 * block-level content while maintaining inline positioning in the document flow.
 */
public class InlineBlock extends InlineWidget implements BlockContainer,
        HasBgColor, HasBorder, HasWidth, HasHeight, HasMargin, HasPadding,
        HandlesPointerEvents
{
    /**
     * Returns the default style instance used by inline blocks.
     *
     * @return the singleton default {@link InlineBlockStyle} instance
     */
    public static InlineBlockStyle getDefaultStyle() {
        return InlineBlockStyle.DEFAULT;
    }

    /**
     * List of child widgets.
     */
    private final List<BlockWidget> children = new ArrayList<>();

    /**
     * Constructs a new inline block with the default style.
     */
    public InlineBlock() {
        super(getDefaultStyle());
    }

    /**
     * Constructs a new inline block with the specified style.
     *
     * @param style the block style to use
     */
    public InlineBlock(final InlineBlockStyle style) {
        super(style);
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
        return "inline block";
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
