/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;

/**
 * Listener that tracks changes in an inline widget size model (e.g., width or height)
 * and sends an instruction to the client to update the corresponding CSS property.
 */
final class InlineWidgetSizeListener implements Listener<InlineWidgetSize> {

    /**
     * Widget associated with the size model.
     */
    private final Widget widget;

    /**
     * Name of the CSS property to update (e.g., "width" or "height").
     */
    private final String name;

    /**
     * Constructs a listener for a given widget and CSS property.
     *
     * @param widget The widget whose size will be updated on the client
     * @param name The name of the CSS property ("width" or "height")
     */
    InlineWidgetSizeListener(final Widget widget, final String name) {
        this.widget = widget;
        this.name = name;
    }

    @Override
    public void dataChanged(final InlineWidgetSize data) {
        // Build and send 'set width' or 'set height' instruction
        Instruction instruction = new Instruction(widget.getWidgetId()) {
            @Override
            protected String getAction() {
                return "set " + name;
            }

            @Override
            protected void fillJsonObject(JsonObject json) {
                json.addString(name, data.getCSSCode());
            }
        };
        widget.sendToClient(instruction);
    }
}