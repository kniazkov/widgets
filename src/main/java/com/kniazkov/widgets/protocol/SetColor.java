/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.UId;

/**
 * An {@link Update} that instructs the client to change the color of a target.
 */
public class SetColor extends Update {
    /**
     * The new color to apply.
     */
    private final Color color;

    /**
     * Creates a new color update.
     *
     * @param target the target ID whose color will change
     * @param color the new color value
     */
    public SetColor(UId target, Color color) {
        super(target);
        this.color = color;
    }

    @Override
    public Update clone() {
        return new SetColor(this.getTargetId(), this.color);
    }

    @Override
    protected String getAction() {
        return "set color";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addElement("color", this.color.toJsonObject());
    }
}
