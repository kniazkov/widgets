/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;

/**
 * Listener that tracks changes in an italic model and sends an instruction
 * to the client to update the italic style of the associated widget.
 */
final class ItalicModelListener implements Listener<Boolean> {

    /**
     * Widget associated with the italic model.
     */
    private final Widget widget;

    /**
     * Constructs a listener for a given widget.
     *
     * @param widget The widget whose italic style will be updated on the client
     */
    ItalicModelListener(final Widget widget) {
        this.widget = widget;
    }

    @Override
    public void dataChanged(final Boolean data) {
        // Build and send 'set italic' instruction
        Instruction instruction = new Instruction(widget.getWidgetId()) {
            @Override
            protected String getAction() {
                return "set italic";
            }

            @Override
            protected void fillJsonObject(JsonObject json) {
                json.addBoolean("italic", data);
            }
        };
        widget.sendToClient(instruction);
    }
}
