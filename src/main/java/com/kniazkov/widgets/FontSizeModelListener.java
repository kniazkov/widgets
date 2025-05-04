/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;

/**
 * Listener that tracks changes in a font size model and sends an instruction
 * to the client to update the font size of the associated widget.
 */
final class FontSizeModelListener implements Listener<FontSize> {

    /**
     * Widget associated with the font size model.
     */
    private final Widget widget;

    /**
     * Constructs a listener for a given widget.
     *
     * @param widget The widget whose font size will be updated on the client
     */
    FontSizeModelListener(final Widget widget) {
        this.widget = widget;
    }

    @Override
    public void dataChanged(final FontSize data) {
        // Build and send 'set font size' instruction
        Instruction instruction = new Instruction(widget.getWidgetId()) {
            @Override
            protected String getAction() {
                return "set font size";
            }

            @Override
            protected void fillJsonObject(JsonObject json) {
                json.addString("font size", data.getCSSCode()); // e.g., "14px"
            }
        };
        widget.sendToClient(instruction);
    }
}
