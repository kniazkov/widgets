package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

/**
 * Listener to follow text model data updates and send instructions to clients.
 */
final class TextModelListener implements Listener<String> {
    /**
     * Widget containing model.
     */
    private final Widget widget;

    /**
     * Constructor.
     * @param widget Widget containing model
     */
    TextModelListener(final Widget widget) {
        this.widget = widget;
    }

    @Override
    public void dataChanged(final @NotNull String data) {
        final Instruction instruction = new Instruction(this.widget.getWidgetId()) {
            @Override
            protected @NotNull String getAction() {
                return "set text";
            }

            @Override
            protected void fillJsonObject(final @NotNull JsonObject json) {
                json.addString("text", data);
            }
        };
        this.widget.sendToClient(instruction);
    }
}
