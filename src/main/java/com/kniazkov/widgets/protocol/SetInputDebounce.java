/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * Configures bounded trailing-edge debounce for committed text input.
 */
public final class SetInputDebounce extends Update {
    /**
     * Delay, zero for immediate delivery.
     */
    private final int milliseconds;

    /**
     * Creates an editor configuration update.
     * @param widget editor identity
     * @param milliseconds delay in the range 0..2000
     */
    public SetInputDebounce(final RMId widget, final int milliseconds) {
        super(widget);
        if (milliseconds < 0 || milliseconds > 2000) {
            throw new IllegalArgumentException("Debounce must be in 0..2000 milliseconds");
        }
        this.milliseconds = milliseconds;
    }

    @Override
    public Update clone() {
        return new SetInputDebounce(this.getWidgetId(), this.milliseconds);
    }

    @Override
    protected String getAction() {
        return "set input debounce";
    }

    @Override
    protected void fillJsonObject(final JsonObject object) {
        object.addNumber("milliseconds", this.milliseconds);
    }
}
