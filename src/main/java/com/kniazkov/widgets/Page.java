/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * The interface presents a method for creating a web page using a set of widgets.
 * To use this library, the user must implement at least one page.
 */
public interface Page {
    /**
     * Creates a web page by adding widgets to the root widget.
     * @param root Root widget
     */
    void create(final @NotNull RootWidget root);
}
