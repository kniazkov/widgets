/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import java.util.Locale;
import java.util.Objects;

/**
 * Block children in source order with a configurable axis, cross-axis alignment and gap.
 */
public final class Stack extends Panel {
    /**
     * Main layout axis.
     */
    public enum Direction {
        /**
         * Top to bottom.
         */
        VERTICAL,
        /**
         * Inline direction.
         */
        HORIZONTAL
    }

    /**
     * Logical cross-axis alignment for children.
     */
    public enum Alignment {
        /**
         * Align at the logical start.
         */
        START,
        /**
         * Align at the center.
         */
        CENTER,
        /**
         * Align at the logical end.
         */
        END,
        /**
         * Stretch along the cross axis.
         */
        STRETCH
    }

    /**
     * Creates a vertical stack.
     */
    public Stack() {
    }

    /**
     * Creates a stack on the requested axis.
     * @param direction main axis
     */
    @SuppressWarnings("this-escape")
    public Stack(final Direction direction) {
        this.setDirection(direction);
    }

    /**
     * Changes the main axis without reattaching children.
     * @param direction main axis
     */
    public void setDirection(final Direction direction) {
        this.getModel(State.ANY, Property.STACK_DIRECTION)
            .setData(Objects.requireNonNull(direction).name().toLowerCase(Locale.ROOT));
    }

    /**
     * Changes cross-axis alignment without changing source or Tab order.
     * @param alignment child alignment
     */
    public void setAlignment(final Alignment alignment) {
        this.getModel(State.ANY, Property.STACK_ALIGNMENT)
            .setData(Objects.requireNonNull(alignment).name().toLowerCase(Locale.ROOT));
    }

    /**
     * Controls wrapping in horizontal mode; wraps by default.
     * @param value whether a horizontal line can wrap
     */
    public void setWrap(final boolean value) {
        this.getModel(State.ANY, Property.STACK_WRAP).setData(value);
    }

    /**
     * Changes the spacing between children and wrapped rows.
     * @param pixels gap in CSS pixels (0–128)
     */
    public void setGap(final int pixels) {
        LayoutSizes.gap(this, pixels);
    }

    @Override
    public String getType() {
        return "stack";
    }
}
