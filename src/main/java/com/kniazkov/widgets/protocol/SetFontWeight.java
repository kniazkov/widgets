/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.UId;

/**
 * An {@link Update} that instructs the client to change the weight of the font of a widget.
 */
public class SetFontWeight extends Update {
    /**
     * The new font weight to apply.
     */
    private final FontWeight fontWeight;

    /**
     * Creates a new font weight update.
     *
     * @param widget the widget ID whose font weight will change
     * @param fontWeight the new font weight value
     */
    public SetFontWeight(UId widget, FontWeight fontWeight) {
        super(widget);
        this.fontWeight = fontWeight;
    }

    @Override
    public Update clone() {
        return new SetFontWeight(this.getWidgetId(), this.fontWeight);
    }

    @Override
    protected String getAction() {
        return "set font weight";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addNumber("font weight", this.fontWeight.getWeight());
    }
}
