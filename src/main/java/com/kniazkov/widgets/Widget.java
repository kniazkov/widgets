/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A widget, that is, a user interface element.
 */
public interface Widget {
    /**
     * Returns the unique identifier of the widget.
     * @return Identifier
     */
    UId getId();
}
