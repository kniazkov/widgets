/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;

/**
 * Listener that tracks changes in a font weight model and sends an instruction
 * to the client to update the font weight of the associated widget.
 */
final class FontWeightModelListener implements Listener<FontWeight> {

    /**
     * Widget associated with the font weight model.
     */
    private final Widget widget;

    /**
     * Constructs a listener for a given widget.
     *
     * @param widget The widget whose font weight will be updated on the client
     */
    FontWeightModelListener(final Widget widget) {
        this.widget = widget;
    }

    @Override
    public void dataChanged(final FontWeight data) {
        // Build and send 'set font weight' instruction
        Instruction instruction = new Instruction(widget.getWidgetId()) {
            @Override
            protected String getAction() {
                return "set font weight";
            }

            @Override
            protected void fillJsonObject(JsonObject json) {
                json.addNumber("font weight", data.getWeight());
            }
        };
        widget.sendToClient(instruction);
    }
}
