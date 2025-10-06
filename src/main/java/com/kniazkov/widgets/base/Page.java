/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.widgets.view.RootWidget;

/**
 * Defines a web page in the widget-based UI framework.
 * Every application must implement at least one page that declares its content
 * using widgets. A page is a <b>stateless</b> declarative definition that
 * describes what the client UI should contain.
 * When a new client session starts, the framework automatically creates a
 * {@link RootWidget} and calls {@link #create(RootWidget)} to populate it
 * with widgets forming the page structure.
 */
public interface Page {

    /**
     * Builds the widget tree that defines the structure of this page.
     * Implementations should populate the given {@link RootWidget} with
     * widgets representing the page content. The method is invoked once
     * per client session.
     *
     * @param root the root widget of the page
     */
    void create(RootWidget root);
}
