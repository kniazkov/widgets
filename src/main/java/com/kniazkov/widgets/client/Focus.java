/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.client;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.view.Widget;
import java.util.Objects;

/**
 * Focuses a live widget without scrolling.
 */
public final class Focus implements OnClient {
    /**
     * Target identity, without retaining the widget tree.
     */
    private final String target;

    /**
     * Creates a local UI action.
     * @param target widget in the same page runtime
     */
    public Focus(final Widget<?> target) {
        this.target = Objects.requireNonNull(target, "target").getId().toString();
    }

    @Override
    public String getAction() {
        return "focus widget";
    }

    @Override
    public void fillJsonObject(final JsonObject object) {
        object.addString("target", this.target);
    }
}
