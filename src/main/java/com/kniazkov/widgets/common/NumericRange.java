/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Immutable closed interval of finite doubles. Both endpoints are inclusive.
 * @param lower lower endpoint
 * @param upper upper endpoint
 */
public record NumericRange(double lower, double upper) {
    /**
     * Rejects reversed/nonfinite bounds and canonicalizes signed zero.
     */
    public NumericRange {
        if (!Double.isFinite(lower) || !Double.isFinite(upper) || lower > upper) {
            throw new IllegalArgumentException("Expected finite lower <= upper");
        }
        lower = lower == 0 ? 0 : lower;
        upper = upper == 0 ? 0 : upper;
    }

    /**
     * @param value point to test
     * @return whether the point is inside this interval
     */
    public boolean contains(final double value) {
        return Double.isFinite(value) && lower <= value && value <= upper;
    }

    /**
     * @param other interval to test
     * @return whether the intervals share any point, including an endpoint
     */
    public boolean intersects(final NumericRange other) {
        return lower <= other.upper && other.lower <= upper;
    }
}
