/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.UId;

/**
 * An {@link Update} that instructs the client to change the font face of a widget.
 */
public class SetFontFace extends Update {
    /**
     * The new font face to apply.
     */
    private final FontFace fontFace;

    /**
     * Creates a new font face update.
     *
     * @param widget the widget ID whose font face will change
     * @param fontFace the new font face value
     */
    public SetFontFace(UId widget, FontFace fontFace) {
        super(widget);
        this.fontFace = fontFace;
    }

    @Override
    public Update clone() {
        return new SetFontFace(this.getWidgetId(), this.fontFace);
    }

    @Override
    protected String getAction() {
        return "set font face";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("font face", this.fontFace.getName());
    }
}
