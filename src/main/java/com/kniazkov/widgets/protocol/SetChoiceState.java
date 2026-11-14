/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * Replaces atomic browser-facing choice state.
 */
public final class SetChoiceState extends Update {
    /**
     * Newly constructed snapshot, never mutated after this update is queued.
     */
    private final JsonObject state;

    /**
     * @param widget target ID
     * @param state owned immutable-by-convention snapshot
     */
    public SetChoiceState(final RMId widget, final JsonObject state) {
        super(widget);
        this.state = state;
    }

    @Override
    public Update clone() {
        return new SetChoiceState(this.getWidgetId(), this.state);
    }

    @Override
    protected String getAction() {
        return "set choice state";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addElement("state", this.state);
    }
}
