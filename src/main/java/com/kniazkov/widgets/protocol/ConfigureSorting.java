/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * Configures the duration of sortable child position animations.
 */
public final class ConfigureSorting extends Update {
    /**
     * Animation duration in milliseconds.
     */
    private final int animationDuration;

    /**
     * Creates a sorting animation configuration update.
     * @param widget sortable section identifier
     * @param animationDuration duration in milliseconds
     */
    public ConfigureSorting(final RMId widget, final int animationDuration) {
        super(widget);
        this.animationDuration = animationDuration;
    }

    @Override
    public Update clone() {
        return new ConfigureSorting(this.getWidgetId(), this.animationDuration);
    }

    @Override
    protected String getAction() {
        return "configure sorting";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addNumber("animationDuration", this.animationDuration);
    }
}
