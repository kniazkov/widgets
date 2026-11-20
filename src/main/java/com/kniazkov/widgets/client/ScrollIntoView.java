/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.client;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.view.Widget;
import java.util.Objects;

/**
 * Scrolls a live widget into view without a server round trip.
 */
public final class ScrollIntoView implements OnClient {
    /**
     * Target identity, without retaining the widget tree.
     */
    private final String target;

    /**
     * Creates a local UI action.
     * @param target widget in the same page runtime
     */
    public ScrollIntoView(final Widget<?> target) {
        this.target = Objects.requireNonNull(target, "target").getId().toString();
    }

    @Override
    public String getAction() {
        return "scroll to widget";
    }

    @Override
    public void fillJsonObject(final JsonObject object) {
        object.addString("target", this.target);
    }
}
