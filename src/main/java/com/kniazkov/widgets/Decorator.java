/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * A container that can contain only one widget, and can give that widget some properties.
 */
public abstract class Decorator extends Container {
    /**
     * Returns the widget that is being decorated.
     * @return Widget
     */
    public abstract Widget getChild();

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public @NotNull Widget getChild(final int index) throws IndexOutOfBoundsException {
        if (index == 0) {
            return this.getChild();
        }
        throw new IndexOutOfBoundsException();
    }
}
