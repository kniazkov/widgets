/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.UId;

/**
 * Represents a logical container of {@link Widget}s.
 * A {@code Container} is responsible for holding and managing child widgets. Unlike {@link Widget},
 * a container DOES NOT HAVE to be part of the visual hierarchy itself — it is simply an object
 * that owns or groups widgets.
 */
public interface Container {
    /**
     * Returns the unique identifier of this container.
     *
     * @return the container ID
     */
    UId getId();

    /**
     * Returns the number of widgets currently stored in this container.
     *
     * @return the number of child widgets
     */
    int getChildCount();

    /**
     * Returns the child widget at the specified index.
     *
     * @param index the index of the child (from {@code 0} to {@code getChildCount() - 1})
     * @return the widget at the specified index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    Widget getChild(int index) throws IndexOutOfBoundsException;

    /**
     * Removes a widget from this container.
     * The exact behavior depends on the container implementation: some containers may simply
     * remove the widget; others may replace it with a default widget if empty state is not allowed;
     * as a result, the total number of widgets MAY remain unchanged.
     *
     * @param widget the widget to remove
     */
    void remove(Widget widget);
}
