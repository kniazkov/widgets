/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.UId;
import com.kniazkov.widgets.view.State;

/**
 * An {@link Update} that instructs the client to change the background color of a widget.
 */
public final class SetBgColor extends Update {
    /**
     * The state of the widget in which the change is applied.
     */
    private final State state;

    /**
     * The new background color to apply.
     */
    private final Color color;

    /**
     * Creates a new background color update.
     *
     * @param widget the widget ID whose background color will change
     * @param state the state of the widget in which the change is applied
     * @param color the new color value
     */
    public SetBgColor(UId widget, State state, Color color) {
        super(widget);
        this.state = state;
        this.color = color;
    }

    @Override
    public Update clone() {
        return new SetBgColor(this.getWidgetId(), this.state, this.color);
    }

    @Override
    protected String getAction() {
        return "set background color";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("state", this.state.toString());
        json.addElement("background color", this.color.toJsonObject());
    }
}
