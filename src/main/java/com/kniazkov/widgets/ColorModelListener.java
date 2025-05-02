/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;

/**
 * Listener that tracks changes in a color model and sends an instruction
 * to the client to update the color of the associated widget.
 */
final class ColorModelListener implements Listener<Color> {
    /**
     * Widget associated with the color model.
     */
    private final Widget widget;

    /**
     * Constructs a listener for a given widget.
     *
     * @param widget the widget whose color will be updated on the client
     */
    ColorModelListener(final Widget widget) {
        this.widget = widget;
    }

    @Override
    public void dataChanged(final Color data) {
        // Build and send 'set color' instruction
        Instruction instruction = new Instruction(this.widget.getWidgetId()) {
            @Override
            protected String getAction() {
                return "set color";
            }

            @Override
            protected void fillJsonObject(JsonObject json) {
                json.addElement("color", data.toJsonObject());
            }
        };
        this.widget.sendToClient(instruction);
    }
}
