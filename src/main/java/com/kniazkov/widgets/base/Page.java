/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.widgets.view.RootWidget;
import java.util.Map;

/**
 * Defines a web page in the widget-based UI framework.
 * <p>
 * Every application must implement at least one page that declares its content
 * using widgets. A page is a <b>stateless</b> declarative definition describing what the client
 * UI should contain. When a new client session starts, the framework creates a {@link RootWidget}
 * and invokes {@link #create(RootWidget, Map)} to populate it with widgets and build the visual
 * structure of the page.
 *
 * <p>
 * Pages may also receive URL query parameters. For example, accessing:
 * <pre>
 *   http://host:port/page?id=123&mode=edit
 * </pre>
 * makes the following entries available inside {@code parameters}:
 * <ul>
 *     <li>{@code "id" → "123"}</li>
 *     <li>{@code "mode" → "edit"}</li>
 * </ul>
 *
 * <p>
 * These parameters can be used to customize page content, filter data, preselect values,
 * switch modes, or configure behavior dynamically based on the request.
 */
public interface Page {
    /**
     * Builds the widget tree that defines the structure of this page.
     * <p>
     * Implementations should populate the given {@link RootWidget} with widgets
     * representing the page content. This method is invoked once per client session when the page
     * is requested.
     *
     * <p>
     * The {@code parameters} map contains query string parameters extracted
     * from the request URL. For example, a request like:
     * <pre>
     *   /profile?id=42&tab=info
     * </pre>
     * will result in:
     * <ul>
     *     <li>{@code parameters.get("id")}   → {@code "42"}</li>
     *     <li>{@code parameters.get("tab")}  → {@code "info"}</li>
     * </ul>
     *
     * @param root the root widget of the page
     * @param parameters URL query parameters mapped by name; can be empty, never {@code null}
     */
    void create(RootWidget root, Map<String, String> parameters);
}

