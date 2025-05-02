/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;

/**
 * Listener that tracks changes in a text model and sends an instruction
 * to the client to update the text of the associated widget.
 */
final class TextModelListener implements Listener<String> {
    /**
     * Widget associated with the text model.
     */
    private final Widget widget;

    /**
     * Constructs a listener for a given widget.
     *
     * @param widget The widget whose text will be updated on the client
     */
    TextModelListener(final Widget widget) {
        this.widget = widget;
    }

    @Override
    public void dataChanged(final String data) {
        // Build and send 'set text' instruction
        Instruction instruction = new Instruction(this.widget.getWidgetId()) {
            @Override
            protected String getAction() {
                return "set text";
            }

            @Override
            protected void fillJsonObject(JsonObject json) {
                json.addString("text", data);
            }
        };
        this.widget.sendToClient(instruction);
    }
}
