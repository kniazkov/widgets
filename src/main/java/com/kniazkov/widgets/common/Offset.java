/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import com.kniazkov.json.JsonObject;
import java.util.Objects;

public class Offset {
    private final AbsoluteSize left, right, top, bottom;

    public Offset(final AbsoluteSize size) {
        this(size, size, size, size);
    }

    public Offset(final String size) {
        this(new AbsoluteSize(size));
    }

    public Offset(final AbsoluteSize horizontal, final AbsoluteSize vertical) {
        this(horizontal, horizontal, vertical, vertical);
    }

    public Offset(final String horizontal, final String vertical) {
        this(new AbsoluteSize(horizontal), new AbsoluteSize(vertical));
    }

    public Offset(final AbsoluteSize left, final AbsoluteSize right, final AbsoluteSize top,
                  final AbsoluteSize bottom) {
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
    }

    public Offset(final String left, final String right, final String top, final String bottom) {
       this(
           new AbsoluteSize(left),
           new AbsoluteSize(right),
           new AbsoluteSize(top),
           new AbsoluteSize(bottom)
       );
    }

    public AbsoluteSize getLeft() {
        return this.left;
    }

    public AbsoluteSize getRight() {
        return this.right;
    }

    public AbsoluteSize getTop() {
        return this.top;
    }

    public AbsoluteSize getBottom() {
        return this.bottom;
    }

    public Offset setLeft(final AbsoluteSize newLeft) {
        return new Offset(newLeft, this.right, this.top, this.bottom);
    }

    public JsonObject toJsonObject() {
        JsonObject obj = new JsonObject();
        obj.addString("left", this.left.getCSSCode());
        obj.addString("right", this.right.getCSSCode());
        obj.addString("top", this.top.getCSSCode());
        obj.addString("bottom", this.bottom.getCSSCode());
        return obj;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Offset)) {
            return false;
        }
        final Offset other = (Offset) obj;
        return this.left.equals(other.left) && this.right.equals(other.right) &&
            this.top.equals(other.top) && this.bottom.equals(other.bottom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.left, this.right, this.top, this.bottom);
    }
}
