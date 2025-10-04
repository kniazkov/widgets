/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * A {@link Container} that always contains exactly one {@link Widget}.
 * A {@code Decorator} is a special kind of container that wraps another widget.
 * It guarantees that there is always one child widget inside it. Initially, or after the contained
 * widget is removed, the decorator replaces it with a default placeholder widget, so it is never
 * empty.
 *
 * @param <T> the type of widget contained in this decorator
 */
public interface Decorator<T extends Widget> extends Container {
    @Override
    default int getChildCount() {
        return 1;
    }

    @Override
    default T getChild(final int index) throws IndexOutOfBoundsException {
        if (index != 0) {
            throw new IndexOutOfBoundsException("Decorator has only one child at index 0");
        }
        return this.getChild();
    }

    /**
     * Returns the widget currently wrapped by this decorator.
     * This method never returns {@code null}.
     *
     * @return the contained widget
     */
    T getChild();

    /**
     * Replaces the contained widget with a new one.
     *
     * @param widget the widget to place inside the decorator
     */
    void put(T widget);
}
