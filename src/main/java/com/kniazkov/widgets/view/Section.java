/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.protocol.AppendChild;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A block-level container that holds inline widgets arranged from left to right.
 * A {@code Section} is similar to a paragraph or block element in HTML but is restricted
 * to containing {@link InlineWidget}s. Widgets are appended in order and displayed
 * in a horizontal flow within the section.
 */
public class Section extends BlockWidget implements TypedContainer<InlineWidget> {
    /**
     * List of child widgets.
     */
    private final List<InlineWidget> children = new ArrayList<>();

    /**
     * Creates a new section.
     */
    public Section() {
        super(Style.EMPTY_STYLE);
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

    @Override
    public void handleEvent(String type, Optional<JsonObject> data) {
        // not yet
    }
}
