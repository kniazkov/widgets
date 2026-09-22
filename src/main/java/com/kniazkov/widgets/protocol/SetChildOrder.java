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
 * Publishes the authoritative child order and its revision.
 */
public final class SetChildOrder extends Update {
    /**
     * Child identifiers in display order.
     */
    private final List<String> children;

    /**
     * Container revision.
     */
    private final int revision;

    /**
     * Creates an order update.
     * @param widget container identifier
     * @param children ordered child identifiers
     * @param revision container revision
     */
    public SetChildOrder(final RMId widget, final List<String> children, final int revision) {
        super(widget);
        this.children = List.copyOf(children);
        this.revision = revision;
    }

    @Override
    public Update clone() {
        return new SetChildOrder(this.getWidgetId(), this.children, this.revision);
    }

    @Override
    protected String getAction() {
        return "set child order";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addNumber("revision", this.revision);
        final JsonArray array = json.createArray("children");
        for (final String child : this.children) {
            array.add(new JsonString(child));
        }
    }
}
