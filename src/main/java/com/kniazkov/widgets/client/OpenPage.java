/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.client;

import com.kniazkov.json.JsonObject;
import java.util.Objects;

/**
 * Client action that opens a page in the current browser tab.
 */
public final class OpenPage implements OnClient {
    /**
     * Destination URL.
     */
    private final String href;

    /**
     * Creates a current-tab navigation action.
     *
     * @param href destination URL
     */
    public OpenPage(final String href) {
        this.href = Objects.requireNonNull(href, "href");
    }

    /**
     * Returns the destination URL.
     *
     * @return destination URL
     */
    public String getHref() {
        return this.href;
    }

    @Override
    public String getAction() {
        return "go to page";
    }

    @Override
    public void fillJsonObject(final JsonObject object) {
        object.addString("href", this.href);
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof OpenPage action
            && this.href.equals(action.href);
    }

    @Override
    public int hashCode() {
        return this.href.hashCode();
    }
}
