/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.widgets.common.RMId;

/**
 * Resets the local browser viewport without changing its reactive scale limit.
 */
public final class ResetZoom extends Update {
    /**
     * Creates a viewport reset command.
     * @param widget decorator identifier
     */
    public ResetZoom(final RMId widget) {
        super(widget);
    }

    @Override
    public Update clone() {
        return new ResetZoom(this.getWidgetId());
    }

    @Override
    protected String getAction() {
        return "reset zoom";
    }
}
