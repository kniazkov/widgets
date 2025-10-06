/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;

/**
 * An {@link Update} that instructs the client to place (set) a widget
 * into a specific container (decorator).
 */
public final class SetChild extends Update {
    /**
     * The identifier of the container where the widget is placed.
     */
    private final UId container;

    /**
     * Creates a new "set widget in container" update.
     *
     * @param widget the widget being placed
     * @param container the container that will contain the widget
     */
    public SetChild(final UId widget, final UId container) {
        super(widget);
        this.container = container;
    }

    @Override
    protected String getAction() {
        return "set child";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("container", this.container.toString());
    }
}
