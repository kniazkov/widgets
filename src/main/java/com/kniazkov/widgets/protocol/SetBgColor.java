/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.UId;

/**
 * An {@link Update} that instructs the client to change the background color of a widget.
 */
public class SetBgColor extends Update {
    /**
     * The new background color to apply.
     */
    private final Color color;

    /**
     * Creates a new background color update.
     *
     * @param widget the widget ID whose background color will change
     * @param color the new color value
     */
    public SetBgColor(UId widget, Color color) {
        super(widget);
        this.color = color;
    }

    @Override
    public Update clone() {
        return new SetBgColor(this.getWidgetId(), this.color);
    }

    @Override
    protected String getAction() {
        return "set background color";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addElement("background color", this.color.toJsonObject());
    }
}
