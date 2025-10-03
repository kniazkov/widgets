/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;

/**
 * An {@link Update} that instructs the client to create a new widget.
 */
public final class Create extends Update {
    /**
     * The widget type (e.g. {@code "button"}, {@code "text"}).
     */
    private final String type;


    /**
     * Creates a new "create" update for the given widget and type.
     *
     * @param widget the widget identifier
     * @param type the widget type (e.g. {@code "button"}, {@code "text"})
     */
    public Create(final UId widget, final String type) {
        super(widget);
        this.type = type;
    }

    @Override
    protected String getAction() {
        return "create";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("type", this.type);
    }
}
