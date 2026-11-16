/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * Publishes a navigation widget's owned controls and stable-key selection.
 */
public final class SetNavigation extends Update {
    /**
     * Configuration snapshot, never mutated after publication.
     */
    private final JsonObject snapshot;

    /**
     * @param widget navigation container
     * @param snapshot owned control references
     */
    public SetNavigation(final RMId widget, final JsonObject snapshot) {
        super(widget);
        this.snapshot = snapshot;
    }

    @Override
    public Update clone() {
        return new SetNavigation(this.getWidgetId(), this.snapshot);
    }

    @Override
    protected String getAction() {
        return "set navigation";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addElement("snapshot", this.snapshot);
    }
}
