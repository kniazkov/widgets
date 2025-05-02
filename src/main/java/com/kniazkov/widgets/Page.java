/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Interface for defining a web page using widgets.
 * <p>
 *     Every application must implement at least one page that describes its content.
 *     A page is created by adding widgets to the provided {@link RootWidget}.
 * </p>
 *
 * <p>
 *     Pages are stateless definitions: they are invoked when a new client session is created.
 *     The framework automatically invokes {@link #create(RootWidget)} for each new client.
 * </p>
 */
public interface Page {

    /**
     * Defines the structure of a web page.
     * <p>
     *     Implementations should add widgets to the provided root in order to construct
     *     the page content for a new client session.
     * </p>
     *
     * @param root the root widget of the page
     */
    void create(RootWidget root);
}
