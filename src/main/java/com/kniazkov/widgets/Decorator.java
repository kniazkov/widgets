/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * A container that can contain only one widget, and can give that child some properties.
 * @param <T> Type of decorated widget
 */
public interface Decorator<T extends Widget> extends Container {
    /**
     * Returns the widget that is being decorated.
     * The decorator is never empty. Once created, any decorator always has some widget
     * by default that is decorated .
     * @return Widget
     */
    @NotNull T getChild();

    /**
     * Sets a new child widget in replacing an existing one.
     * @param child New child widget
     */
    void setChild(@NotNull T child);

    @Override
    default int getChildCount() {
        return 1;
    }

    @Override
    @NotNull default Widget getChild(final int index) throws IndexOutOfBoundsException {
        if (index == 0) {
            return this.getChild();
        }
        throw new IndexOutOfBoundsException();
    }
}
