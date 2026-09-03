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
 * Replaces all options of a browser-side drop-down list while preserving their order.
 */
public final class SetDropDownOptions extends Update {
    /**
     * Immutable option snapshot.
     */
    private final List<String> options;

    /**
     * Creates an option-list update.
     *
     * @param widget target widget
     * @param options ordered option snapshot
     */
    public SetDropDownOptions(final RMId widget, final List<String> options) {
        super(widget);
        this.options = List.copyOf(options);
    }

    @Override
    public Update clone() {
        return new SetDropDownOptions(this.getWidgetId(), this.options);
    }

    @Override
    protected String getAction() {
        return "set options";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        final JsonArray array = json.createArray("options");
        for (final String option : this.options) {
            array.add(new JsonString(option));
        }
    }
}
