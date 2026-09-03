/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * Changes the visible text of one option without changing the drop-down list size.
 */
public final class SetDropDownOption extends Update {
    /**
     * Option position.
     */
    private final int index;

    /**
     * New visible text.
     */
    private final String text;

    /**
     * Creates a single-option update.
     *
     * @param widget target widget
     * @param index option position
     * @param text new visible text
     */
    public SetDropDownOption(final RMId widget, final int index, final String text) {
        super(widget);
        this.index = index;
        this.text = text;
    }

    @Override
    public Update clone() {
        return new SetDropDownOption(this.getWidgetId(), this.index, this.text);
    }

    @Override
    protected String getAction() {
        return "set option";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addNumber("index", this.index);
        json.addString("text", this.text);
    }
}
