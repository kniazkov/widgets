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
 * Replaces the ordered hyperlinks opened for carousel positions.
 */
public final class SetCarouselNewTabHrefs extends Update {
    /**
     * Immutable hyperlink snapshot.
     */
    private final List<String> hrefs;

    /**
     * Creates a hyperlink-array update.
     *
     * @param widget target carousel
     * @param hrefs hyperlinks in carousel order
     */
    public SetCarouselNewTabHrefs(final RMId widget, final List<String> hrefs) {
        super(widget);
        this.hrefs = List.copyOf(hrefs);
    }

    @Override
    public Update clone() {
        return new SetCarouselNewTabHrefs(this.getWidgetId(), this.hrefs);
    }

    @Override
    protected String getAction() {
        return "set carousel new tab hrefs";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        final JsonArray array = json.createArray("hrefs");
        for (final String href : this.hrefs) {
            array.add(new JsonString(href));
        }
    }
}
