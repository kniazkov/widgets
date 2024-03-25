/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * A container that can contain only one entity, and can give that child some properties.
 * @param <T> Type of child entities
 */
public interface Decorator<T> extends Container<T> {
    /**
     * Returns the widget that is being decorated.
     * @return Widget
     */
    T getChild();

    @Override
    default int getChildCount() {
        return 1;
    }

    @Override
    @NotNull default T getChild(final int index) throws IndexOutOfBoundsException {
        if (index == 0) {
            return this.getChild();
        }
        throw new IndexOutOfBoundsException();
    }
}
