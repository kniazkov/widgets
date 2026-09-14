/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.json.JsonString;
import com.kniazkov.widgets.common.RMId;
import java.util.List;

/**
 * Replaces the fixed ordered source array of a browser-side carousel.
 */
public final class SetCarouselSources extends Update {
    /**
     * Immutable source snapshot.
     */
    private final List<String> sources;

    /**
     * Creates a source-array update.
     *
     * @param widget target widget
     * @param sources ordered image sources
     */
    public SetCarouselSources(final RMId widget, final List<String> sources) {
        super(widget);
        this.sources = List.copyOf(sources);
    }

    @Override
    public Update clone() {
        return new SetCarouselSources(this.getWidgetId(), this.sources);
    }

    @Override
    protected String getAction() {
        return "set carousel sources";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        final JsonArray array = json.createArray("sources");
        for (final String source : this.sources) {
            array.add(new JsonString(source));
        }
    }
}
