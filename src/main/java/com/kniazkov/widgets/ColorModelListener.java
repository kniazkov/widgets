package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

/**
 * Listener to follow color model data updates and send instructions to clients.
 */
final class ColorModelListener implements Listener<Color> {
    /**
     * Widget containing model.
     */
    private final Widget widget;

    /**
     * Constructor.
     * @param widget Widget containing model
     */
    ColorModelListener(final Widget widget) {
        this.widget = widget;
    }

    @Override
    public void dataChanged(final @NotNull Color data) {
        final Instruction instruction = new Instruction(this.widget.getWidgetId()) {
            @Override
            protected @NotNull String getAction() {
                return "set color";
            }

            @Override
            protected void fillJsonObject(final @NotNull JsonObject json) {
                json.addElement("color", data.toJsonObject());
            }
        };
        this.widget.sendToClient(instruction);
    }
}
