/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * A type-safe {@link Container} that holds widgets of a specific type.
 * The {@code TypedContainer} interface provides stronger typing guarantees by restricting its
 * children to a particular {@link Widget} subtype. The logic of how widgets are arranged
 * or positioned (e.g., added to the left, right, top, or bottom) depends entirely
 * on the container implementation.
 *
 * @param <T> the type of widgets contained in this container
 */
public interface TypedContainer<T extends Widget> extends Container {

    /**
     * Returns the child widget at the specified index.
     *
     * @param index the index of the child (from {@code 0} to {@code getChildCount() - 1})
     * @return the widget at the specified index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    @Override
    T getChild(int index) throws IndexOutOfBoundsException;

    /**
     * Adds a widget to this container. The way the widget is added (for example, appended
     * to the right, inserted above, or placed in a specific slot) depends entirely
     * on the implementation. Some containers may also choose to replace existing widgets
     * instead of increasing the count.
     *
     * @param widget the widget to add
     */
    void add(T widget);
}
