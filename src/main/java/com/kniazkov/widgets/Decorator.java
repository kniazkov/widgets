/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.util.Collections;
import java.util.List;

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
    T getChild();

    @Override
    default List<Widget> getChildren() {
        return Collections.singletonList(this.getChild());
    }

    default  List<T> getTypedChildren() {
        return Collections.singletonList(this.getChild());
    }
}
