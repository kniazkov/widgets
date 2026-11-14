/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.widgets.common.RMId;

/**
 * Permanently releases a detached widget subtree in the browser runtime.
 */
public final class DisposeWidget extends Update {
    /**
     * Creates the terminal browser update for a retired widget.
     *
     * @param widget identifier of the widget subtree root
     */
    public DisposeWidget(final RMId widget) {
        super(widget);
    }

    @Override
    public Update clone() {
        return new DisposeWidget(this.getWidgetId());
    }

    @Override
    protected String getAction() {
        return "dispose widget";
    }
}
