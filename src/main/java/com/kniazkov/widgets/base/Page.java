/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.widgets.view.RootWidget;

/**
 * Defines a web page in the widget-based UI framework.
 * <p>
 * Every application must implement at least one page that declares its content
 * using widgets. A page is a <b>stateless</b> declarative definition describing what the client
 * UI should contain. When a new client session starts, the framework creates a {@link RootWidget}
 * and invokes {@link #create(RootWidget, PageSettings)} to populate it with widgets and build the
 * visual structure of the page.
 */
public interface Page {
    /**
     * Builds the widget tree that defines the structure of this page.
     * <p>
     * Implementations should populate the given {@link RootWidget} with widgets
     * representing the page content. This method is invoked once per client session when the page
     * is requested.
     *
     * @param root the root widget of the page
     * @param settings the page settings containing browser identifier and URL parameters;
     *  never {@code null}
     */
    void create(RootWidget root, PageSettings settings);
}
