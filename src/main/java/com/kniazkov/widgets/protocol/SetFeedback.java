/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * Replaces a feedback widget's presentation without moving keyboard focus.
 */
public final class SetFeedback extends Update {
    /**
     * Immutable after publication.
     */
    private final JsonObject snapshot;
    /**
     * Creates an update.
     * @param widget target identity
     * @param snapshot immutable presentation
     */
    public SetFeedback(final RMId widget, final JsonObject snapshot) {
        super(widget);
        this.snapshot = snapshot;
    }
    @Override
    public Update clone() {
        return new SetFeedback(this.getWidgetId(), this.snapshot);
    }
    @Override
    protected String getAction() {
        return "set feedback";
    }
    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addElement("snapshot", this.snapshot);
    }
}
