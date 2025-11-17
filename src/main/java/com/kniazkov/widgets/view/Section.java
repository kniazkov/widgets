/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.protocol.AppendChild;
import java.util.ArrayList;
import java.util.List;

/**
 * A block-level container that holds inline widgets arranged from left to right.
 * A {@code Section} is similar to a paragraph or block element in HTML but is restricted
 * to containing {@link InlineWidget}s. Widgets are appended in order and displayed
 * in a horizontal flow within the section.
 */
public class Section extends BlockWidget implements TypedContainer<InlineWidget>,
        HasHorizontalAlignment {
    /**
     * Returns the default style instance used by sections.
     *
     * @return the singleton default {@link SectionStyle} instance
     */
    public static SectionStyle getDefaultStyle() {
        return SectionStyle.DEFAULT;
    }

    /**
     * List of child widgets.
     */
    private final List<InlineWidget> children = new ArrayList<>();

    /**
     * Creates a new section.
     */
    public Section() {
        this(getDefaultStyle());
    }

    /**
     * Creates a new section with specified style.
     */
    public Section(final SectionStyle style) {
        super(style);
    }

    @Override
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public InlineWidget getChild(int index) throws IndexOutOfBoundsException {
        return this.children.get(index);
    }

    @Override
    public void remove(Widget widget) {
        if (this.children.remove(widget)) {
            widget.setParent(null);
        }
    }

    @Override
    public void add(InlineWidget widget) {
        this.children.add(widget);
        widget.setParent(this);
        pushUpdate(new AppendChild(widget.getId(), this.getId()));
    }

    @Override
    public String getType() {
        return "section";
    }

    /**
     * Sets a new widget style.
     *
     * @param style new widget style
     */
    public void setStyle(SectionStyle style) {
        super.setStyle(style);
    }
}
