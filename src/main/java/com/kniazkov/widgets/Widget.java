/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A widget, that is, a user interface element.
 */
public abstract class Widget {
    /**
     * Widget unique identifier.
     */
    private final UId widgetId;

    /**
     * Constructor.
     */
    public Widget() {
        this.widgetId = UId.create();
    }

    /**
     * Returns the unique identifier of the widget.
     * @return Identifier
     */
    protected UId getWidgetId() {
        return this.widgetId;
    }
}
