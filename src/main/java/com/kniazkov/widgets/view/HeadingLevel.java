/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Document heading levels for a styled text widget.
 */
public enum HeadingLevel {
    /**
     * Ordinary inline text.
     */
    PLAIN(0),
    /**
     * Page heading.
     */
    H1(1),
    /**
     * Section heading.
     */
    H2(2),
    /**
     * Subsection heading.
     */
    H3(3),
    /**
     * Fourth-level heading.
     */
    H4(4),
    /**
     * Fifth-level heading.
     */
    H5(5),
    /**
     * Sixth-level heading.
     */
    H6(6);

    /**
     * Numeric ARIA heading level, or zero for ordinary text.
     */
    private final int value;

    /**
     * Initializes a heading level.
     * @param value numeric level
     */
    HeadingLevel(final int value) {
        this.value = value;
    }

    /**
     * Returns the numeric level transmitted to the browser.
     * @return heading level, or zero for plain text
     */
    int getValue() {
        return this.value;
    }
}
