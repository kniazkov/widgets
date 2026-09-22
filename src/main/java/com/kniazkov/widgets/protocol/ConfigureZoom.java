/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * Configures the zoom limit and resets the browser viewport.
 */
public final class ConfigureZoom extends Update {
    /**
     * Maximum scale.
     */
    private final double maxScale;

    /**
     * Creates a zoom configuration update.
     * @param widget decorator identifier
     * @param maxScale maximum scale
     */
    public ConfigureZoom(final RMId widget, final double maxScale) {
        super(widget);
        this.maxScale = maxScale;
    }

    @Override
    public Update clone() {
        return new ConfigureZoom(this.getWidgetId(), this.maxScale);
    }

    @Override
    protected String getAction() {
        return "configure zoom";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addNumber("maxScale", this.maxScale);
    }
}
