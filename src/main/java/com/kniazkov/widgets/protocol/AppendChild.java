/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;

/**
 * An {@link Update} that instructs the client to add a widget to a specific container.
 */
public final class AppendChild extends Update {
    /**
     * The identifier of the container to which the widget is added.
     */
    private final UId container;

    /**
     * Creates a new "add widget to container" update.
     *
     * @param widget the widget being added
     * @param container the container that receives the widget
     */
    public AppendChild(final UId widget, final UId container) {
        super(widget);
        this.container = container;
    }

    @Override
    protected String getAction() {
        return "append child";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("container", this.container.toString());
    }
}