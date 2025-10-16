/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;
import com.kniazkov.widgets.common.WidgetSize;

/**
 * An {@link Update} that instructs the client to change the width of a target.
 */
public class SetWidth<T extends WidgetSize> extends Update {
    /**
     * The new width to apply.
     */
    private final T width;

    /**
     * Creates a new width update.
     *
     * @param target the target ID whose width will change
     * @param width the new width value
     */
    public SetWidth(UId target, T width) {
        super(target);
        this.width = width;
    }

    @Override
    public Update clone() {
        return new SetWidth<T>(this.getTargetId(), this.width);
    }

    @Override
    protected String getAction() {
        return "set width";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("width", this.width.getCSSCode());
    }
}
