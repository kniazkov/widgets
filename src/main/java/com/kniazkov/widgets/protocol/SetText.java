/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;

/**
 * An {@link Update} that instructs the client to change the target (widget) text.
 */
public class SetText extends Update {
    /**
     * The new text to apply.
     */
    private final String text;

    /**
     * Creates a new text update.
     *
     * @param target the target ID whose text will change
     * @param text the new text value
     */
    public SetText(UId target, String text) {
        super(target);
        this.text = text;
    }

    @Override
    public Update clone() {
        return new SetText(this.getTargetId(), this.text);
    }

    @Override
    protected String getAction() {
        return "set text";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("text", this.text);
    }
}
