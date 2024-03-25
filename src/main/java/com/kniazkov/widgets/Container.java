/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * A widget that contains other widgets.
 */
public abstract class Container extends Widget {
    /**
     * Returns the number of child widgets.
     * @return Number of child widgets
     */
    public abstract int getChildCount();

    /**
     * Returns the child widget by its index.
     * @param index Index
     * @return Child widget
     */
    public abstract @NotNull Widget getChild(final int index) throws IndexOutOfBoundsException;
}
