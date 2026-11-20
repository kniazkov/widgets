/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Responsive CSS grid sized against its own container rather than a device classification.
 */
public final class ResponsiveGrid extends Panel {
    /**
     * Creates automatic equal-width tracks, with no fixed maximum.
     */
    public ResponsiveGrid() {
    }

    /**
     * Sets preferred minimum and maximum column widths; the minimum can shrink to fit.
     * Passing zero for maximum shares available space equally. A fixed maximum can leave
     * space at the logical end and controls CSS auto-fit track counting.
     * @param minimum minimum preferred width in CSS pixels (40–2048)
     * @param maximum fixed maximum in CSS pixels, or zero for flexible tracks
     */
    public void setColumnWidths(final int minimum, final int maximum) {
        if (minimum < 40 || minimum > 2048 || maximum < 0 || maximum > 2048
                || maximum != 0 && maximum < minimum) {
            throw new IllegalArgumentException("Invalid grid column widths");
        }
        this.getModel(State.ANY, Property.GRID_MIN_WIDTH).setData(minimum);
        this.getModel(State.ANY, Property.GRID_MAX_WIDTH).setData(maximum);
    }

    /**
     * Changes spacing between repeated tracks and rows.
     * @param pixels gap in CSS pixels (0–128)
     */
    public void setGap(final int pixels) {
        LayoutSizes.gap(this, pixels);
    }

    @Override
    public String getType() {
        return "responsive grid";
    }
}
