/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * Replaces a slider's numeric snapshot.
 */
public final class SetSlider extends Update {
    /**
     * Newly built snapshot, not mutated once queued.
     */
    private final JsonObject snapshot;

    /**
     * Creates an update for a slider.
     * @param widget target
     * @param snapshot current state
     */
    public SetSlider(final RMId widget, final JsonObject snapshot) {
        super(widget);
        this.snapshot = snapshot;
    }

    @Override
    public Update clone() {
        return new SetSlider(this.getWidgetId(), this.snapshot);
    }

    @Override
    protected String getAction() {
        return "set slider";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addElement("snapshot", this.snapshot);
    }
}
