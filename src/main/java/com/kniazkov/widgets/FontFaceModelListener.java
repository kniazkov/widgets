/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;

/**
 * Listener that tracks changes in a font face model and sends an instruction
 * to the client to update the font face of the associated widget.
 */
final class FontFaceModelListener implements Listener<FontFace> {
    /**
     * Widget associated with the font face model.
     */
    private final Widget widget;

    /**
     * Constructs a listener for a given widget.
     *
     * @param widget The widget whose font face will be updated on the client
     */
    FontFaceModelListener(final Widget widget) {
        this.widget = widget;
    }

    @Override
    public void dataChanged(final FontFace data) {
        // Build and send 'set font face' instruction
        Instruction instruction = new Instruction(widget.getWidgetId()) {
            @Override
            protected String getAction() {
                return "set font face";
            }

            @Override
            protected void fillJsonObject(JsonObject json) {
                json.addString("font face", data.getName());
            }
        };
        widget.sendToClient(instruction);
    }
}
