/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * An {@link Update} that instructs the client to remove a widget from a specific container.
 */
public final class RemoveChild extends Update {
    /**
     * The identifier of the container from which the widget is removed.
     */
    private final RMId container;

    /**
     * Creates a new "remove widget from container" update.
     *
     * @param widget the widget being removed
     * @param container the container from which the widget is removed
     */
    public RemoveChild(final RMId widget, final RMId container) {
        super(widget);
        this.container = container;
    }

    @Override
    public Update clone() {
        return new RemoveChild(this.getWidgetId(), this.container);
    }

    @Override
    protected String getAction() {
        return "remove child";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("container", this.container.toString());
    }
}
