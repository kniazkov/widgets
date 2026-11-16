/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * Configures native temporal hints; server models remain authoritative validators.
 */
public final class SetTemporalInput extends Update {
    /**
     * Immutable configuration snapshot.
     */
    private final JsonObject snapshot;

    /**
     * @param widget target field
     * @param snapshot native attributes
     */
    public SetTemporalInput(final RMId widget, final JsonObject snapshot) {
        super(widget);
        this.snapshot = snapshot;
    }

    @Override
    public Update clone() {
        return new SetTemporalInput(this.getWidgetId(), this.snapshot);
    }

    @Override
    protected String getAction() {
        return "set temporal input";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addElement("snapshot", this.snapshot);
    }
}
