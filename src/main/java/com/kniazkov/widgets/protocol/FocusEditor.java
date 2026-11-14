/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * One-shot focus update for an invalid editor in a form.
 */
public final class FocusEditor extends Update {
    /**
     * Constructor.
     * @param widget target editor
     */
    public FocusEditor(final RMId widget) {
        super(widget);
    }

    @Override
    public Update clone() {
        return new FocusEditor(this.getWidgetId());
    }

    @Override
    protected String getAction() {
        return "focus editor";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        /*
         * The target is the only argument.
         */
    }
}
