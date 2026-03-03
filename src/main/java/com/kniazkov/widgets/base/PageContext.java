/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.util.Map;
import java.util.UUID;

/**
 * Context of a page creation request.
 * <p>
 * Provides request- and client-specific information available when a {@link Page}
 * builds its widget tree. A new {@code PageContext} instance is created for each
 * page request.
 */
public class PageContext {
    /**
     * A unique identifier for the browser/client instance.
     * <p>
     * This UUID is generated once per browser installation and persists across sessions
     * and page reloads, as long as the browser's local storage is not cleared.
     * It can be used to maintain anonymous state or preferences for the client.
     */
    public UUID browserId;

    /**
     * URL query parameters extracted from the request.
     * <p>
     * For example, a request to {@code /page?id=123&mode=edit} results in a map containing:
     * <ul>
     *     <li>{@code "id" → "123"}</li>
     *     <li>{@code "mode" → "edit"}</li>
     * </ul>
     * The map is never {@code null} but may be empty if no parameters were supplied.
     */
    public Map<String, String> parameters;
}
