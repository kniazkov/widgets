/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.util.Map;

/**
 * Explicit opt-in to an anonymous, read-only HTML representation.
 * The provider must return public data only, without authentication or side effects.
 * It is called independently for HTTP bootstrap and interactive page creation.
 * Ordinary Page implementations never invoke this contract.
 */
public interface PublicPage extends Page {
    /**
     * Builds public content from untrusted URL parameters, without a browser session.
     *
     * @param parameters immutable query parameters; validate identifiers before loading data
     * @return immutable public document, never null
     */
    PublicDocument publicDocument(Map<String, String> parameters);
}
