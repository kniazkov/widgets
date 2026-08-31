/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Standard CSS transition timing functions.
 */
public enum TimingFunction {
    /**
     * Constant animation speed.
     */
    LINEAR("linear"),
    /**
     * Browser-standard eased motion.
     */
    EASE("ease"),
    /**
     * Slow start.
     */
    EASE_IN("ease-in"),
    /**
     * Slow finish.
     */
    EASE_OUT("ease-out"),
    /**
     * Slow start and finish.
     */
    EASE_IN_OUT("ease-in-out");

    /**
     * CSS keyword.
     */
    private final String cssCode;

    /**
     * Creates a timing function.
     *
     * @param cssCode CSS keyword
     */
    TimingFunction(final String cssCode) {
        this.cssCode = cssCode;
    }

    /**
     * Returns the CSS keyword.
     *
     * @return CSS timing-function value
     */
    public String getCSSCode() {
        return this.cssCode;
    }

    @Override
    public String toString() {
        return this.cssCode;
    }
}
