/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Click event.
 */
final class ClickEvent implements Event {
    /**
     * Widget identifier read from JSON object.
     */
    private String widgetId;

    @Override
    public @NotNull UId getWidgetId() {
        return UId.parse(this.widgetId);
    }

    @Override
    public void apply(final @NotNull Widget widget) {
        if (widget instanceof Clickable) {
            ((Clickable) widget).clicked();
        }
    }
}
