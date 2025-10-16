/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.UId;

/**
 * An {@link Update} that instructs the client to change the size of the font of a target.
 */
public class SetFontSize extends Update {
    /**
     * The new font size to apply.
     */
    private final FontSize fontSize;

    /**
     * Creates a new font size update.
     *
     * @param target the target ID whose font size will change
     * @param fontSize the new font size value
     */
    public SetFontSize(UId target, FontSize fontSize) {
        super(target);
        this.fontSize = fontSize;
    }

    @Override
    public Update clone() {
        return new SetFontSize(this.getTargetId(), this.fontSize);
    }

    @Override
    protected String getAction() {
        return "set font size";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("font size", this.fontSize.getCSSCode());
    }
}
