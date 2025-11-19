/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Represents a relative widget size expressed in percentages (0% to 100%).
 */
public final class RelativeSize implements WidgetSize {
    /**
     * The percentage value.
     */
    private final float percent;

    /**
     * Creates a new relative size.
     *
     * @param percent the percentage value (0–100)
     * @throws IllegalArgumentException if the percentage is outside the valid range
     */
    public RelativeSize(final float percent) {
        if (percent < 0f || percent > 100f) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100");
        }
        this.percent = percent;
    }

    /**
     * Returns the percentage value.
     *
     * @return the percentage (0–100)
     */
    public float getPercent() {
        return this.percent;
    }

    @Override
    public String getCSSCode() {
        return this.percent + "%";
    }

    @Override
    public String toString() {
        return getCSSCode();
    }
}
