/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;

/**
 * An {@link Update} that instructs the client to remove a widget from a specific container.
 */
public final class RemoveChild extends Update {
    /**
     * The identifier of the container from which the widget is removed.
     */
    private final UId container;

    /**
     * Creates a new "remove widget from container" update.
     *
     * @param widget the widget being removed
     * @param container the container from which the widget is removed
     */
    public RemoveChild(final UId widget, final UId container) {
        super(widget);
        this.container = container;
    }

    @Override
    protected String getAction() {
        return "remove widget from container";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("container", this.container.toString());
    }
}
