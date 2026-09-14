/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * Changes the hyperlink opened for one carousel position.
 */
public final class SetCarouselNewTabHref extends Update {
    /**
     * Carousel position.
     */
    private final int index;

    /**
     * New hyperlink.
     */
    private final String href;

    /**
     * Creates a single-hyperlink update.
     *
     * @param widget target carousel
     * @param index carousel position
     * @param href hyperlink, or an empty string to disable opening
     */
    public SetCarouselNewTabHref(final RMId widget, final int index, final String href) {
        super(widget);
        this.index = index;
        this.href = href;
    }

    @Override
    public Update clone() {
        return new SetCarouselNewTabHref(this.getWidgetId(), this.index, this.href);
    }

    @Override
    protected String getAction() {
        return "set carousel new tab href";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addNumber("index", this.index);
        json.addString("href", this.href);
    }
}
