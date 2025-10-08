/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.UId;

/**
 * An {@link Update} that instructs the client to change the italic style
 * of the text in a widget.
 */
public class SetItalic extends Update {
    /**
     * Whether the text should be italicized.
     */
    private final boolean italic;

    /**
     * Creates a new "set italic" update.
     *
     * @param widget the target widget ID
     * @param italic {@code true} to enable italic text, {@code false} to disable it
     */
    public SetItalic(UId widget, boolean italic) {
        super(widget);
        this.italic = italic;
    }

    @Override
    public Update clone() {
        return new SetItalic(this.getWidgetId(), this.italic);
    }

    @Override
    protected String getAction() {
        return "set italic";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addBoolean("italic", this.italic);
    }
}
