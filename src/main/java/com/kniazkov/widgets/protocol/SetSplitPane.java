/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * Replaces a split pane's layout snapshot.
 */
public final class SetSplitPane extends Update {
    /**
     * Newly built snapshot, not mutated once queued.
     */
    private final JsonObject snapshot;

    /**
     * Creates an update for a split pane.
     * @param widget target
     * @param snapshot current state
     */
    public SetSplitPane(final RMId widget, final JsonObject snapshot) {
        super(widget);
        this.snapshot = snapshot;
    }

    @Override
    public Update clone() {
        return new SetSplitPane(this.getWidgetId(), this.snapshot);
    }

    @Override
    protected String getAction() {
        return "set split pane";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addElement("snapshot", this.snapshot);
    }
}
