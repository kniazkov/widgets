/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Shared sizing rules for adaptive layout components.
 */
final class LayoutSizes {
    /**
     * Utility class.
     */
    private LayoutSizes() {
    }

    /**
     * Validates and applies the common layout gap.
     * @param widget adaptive container
     * @param pixels gap in CSS pixels
     */
    static void gap(final Widget<?> widget, final int pixels) {
        if (pixels < 0 || pixels > 128) {
            throw new IllegalArgumentException("Invalid layout gap");
        }
        widget.getModel(State.ANY, Property.LAYOUT_GAP).setData(pixels);
    }
}
