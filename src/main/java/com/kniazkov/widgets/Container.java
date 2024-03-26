/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * An entity that contains widgets.
 */
public interface Container {
    /**
     * Returns the number of child widgets.
     * @return Number of child widgets
     */
    int getChildCount();

    /**
     * Returns the child widget by its index.
     * @param index Index
     * @return Child entity
     */
    @NotNull Widget getChild(final int index) throws IndexOutOfBoundsException;
}
