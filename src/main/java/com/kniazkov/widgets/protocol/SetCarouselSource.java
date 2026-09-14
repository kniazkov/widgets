/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * Changes one image source without changing carousel positions.
 */
public final class SetCarouselSource extends Update {
    /**
     * Source position.
     */
    private final int index;

    /**
     * New serialized image source.
     */
    private final String source;

    /**
     * Creates a single-source update.
     *
     * @param widget target widget
     * @param index source position
     * @param source new serialized source
     */
    public SetCarouselSource(final RMId widget, final int index, final String source) {
        super(widget);
        this.index = index;
        this.source = source;
    }

    @Override
    public Update clone() {
        return new SetCarouselSource(this.getWidgetId(), this.index, this.source);
    }

    @Override
    protected String getAction() {
        return "set carousel source";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addNumber("index", this.index);
        json.addString("source", this.source);
    }
}
