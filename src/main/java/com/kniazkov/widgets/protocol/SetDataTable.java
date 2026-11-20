/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * Replaces a paged table's bounded display snapshot.
 */
public final class SetDataTable extends Update {
    /**
     * Newly built snapshot, not mutated once queued.
     */
    private final JsonObject snapshot;

    /**
     * Creates an update for a single table.
     * @param widget target
     * @param snapshot current state
     */
    public SetDataTable(final RMId widget, final JsonObject snapshot) {
        super(widget);
        this.snapshot = snapshot;
    }

    @Override
    public Update clone() {
        return new SetDataTable(this.getWidgetId(), this.snapshot);
    }

    @Override
    protected String getAction() {
        return "set data table";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addElement("snapshot", this.snapshot);
    }
}
