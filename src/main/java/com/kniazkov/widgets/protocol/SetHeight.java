/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;
import com.kniazkov.widgets.common.WidgetSize;

/**
 * An {@link Update} that instructs the client to change the height of a widget.
 */
public class SetHeight<T extends WidgetSize> extends Update {
    /**
     * The new height to apply.
     */
    private final T height;

    /**
     * Creates a new height update.
     *
     * @param widget the widget ID whose height will change
     * @param height the new height value
     */
    public SetHeight(UId widget, T height) {
        super(widget);
        this.height = height;
    }

    @Override
    public Update clone() {
        return new SetHeight<T>(this.getWidgetId(), this.height);
    }

    @Override
    protected String getAction() {
        return "set height";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("height", this.height.getCSSCode());
    }
}
