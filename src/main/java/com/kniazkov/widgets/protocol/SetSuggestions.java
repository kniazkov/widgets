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
 * Replaces all suggestions of a browser-side editable field while preserving their order.
 */
public final class SetSuggestions extends Update {
    /**
     * Immutable suggestion snapshot.
     */
    private final List<String> suggestions;

    /**
     * Creates a suggestion-list update.
     *
     * @param widget target widget
     * @param suggestions ordered suggestion snapshot
     */
    public SetSuggestions(final RMId widget, final List<String> suggestions) {
        super(widget);
        this.suggestions = List.copyOf(suggestions);
    }

    @Override
    public Update clone() {
        return new SetSuggestions(this.getWidgetId(), this.suggestions);
    }

    @Override
    protected String getAction() {
        return "set suggestions";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        final JsonArray array = json.createArray("suggestions");
        for (final String suggestion : this.suggestions) {
            array.add(new JsonString(suggestion));
        }
    }
}
