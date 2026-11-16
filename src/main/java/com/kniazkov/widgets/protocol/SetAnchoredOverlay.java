/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * Publishes an anchored overlay's owner, placement and generation.
 */
public final class SetAnchoredOverlay extends Update {
    /**
     * Configuration snapshot, never mutated after publication.
     */
    private final JsonObject snapshot;

    /**
     * @param widget overlay container
     * @param snapshot owned control references
     */
    public SetAnchoredOverlay(final RMId widget, final JsonObject snapshot) {
        super(widget);
        this.snapshot = snapshot;
    }

    @Override
    public Update clone() {
        return new SetAnchoredOverlay(this.getWidgetId(), this.snapshot);
    }

    @Override
    protected String getAction() {
        return "set anchored overlay";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addElement("snapshot", this.snapshot);
    }
}
