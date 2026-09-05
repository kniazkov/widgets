/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * Instructs the client to insert a widget at a specific child position.
 */
public final class InsertChild extends Update {
    /**
     * Target container.
     */
    private final RMId container;

    /**
     * Zero-based position in the container.
     */
    private final int index;

    /**
     * Creates an insertion update.
     *
     * @param widget widget being inserted
     * @param container target container
     * @param index child position
     */
    public InsertChild(final RMId widget, final RMId container, final int index) {
        super(widget);
        this.container = container;
        this.index = index;
    }

    @Override
    public Update clone() {
        return new InsertChild(this.getWidgetId(), this.container, this.index);
    }

    @Override
    protected String getAction() {
        return "insert child";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("container", this.container.toString());
        json.addNumber("index", this.index);
    }
}
