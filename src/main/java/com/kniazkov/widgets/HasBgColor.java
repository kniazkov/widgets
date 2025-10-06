/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Color;

/**
 * An entity that has a background color.
 * <p>
 *     This interface defines access to a background {@link Color} managed through a {@link Model}.
 *     It is designed to coexist with {@link HasColor}, allowing components to expose both
 *     foreground and background color separately.
 * </p>
 */
public interface HasBgColor {
    /**
     * Returns the model responsible for storing and managing the background color.
     *
     * @return Model containing the background color
     */
    Model<Color> getBackgroundColorModel();

    /**
     * Sets the model responsible for storing and managing the background color.
     *
     * @param model Model that provides the background color
     */
    void setBackgroundColorModel(Model<Color> model);

    /**
     * Returns the current background color from the model.
     *
     * @return Background color
     */
    default Color getBackgroundColor() {
        return this.getBackgroundColorModel().getData();
    }

    /**
     * Sets a new background color via the model.
     *
     * @param color Background color to assign
     */
    default void setBackgroundColor(Color color) {
        this.getBackgroundColorModel().setData(color);
    }

    /**
     * Listener that tracks changes in a background color model and sends an instruction
     * to the client to update the background color of the associated widget.
     */
    final class BgColorModelListener implements Listener<Color> {

        /**
         * Widget associated with the background color model.
         */
        private final Widget widget;

        /**
         * Constructs a listener for a given widget.
         *
         * @param widget the widget whose background color will be updated on the client
         */
        BgColorModelListener(final Widget widget) {
            this.widget = widget;
        }

        @Override
        public void dataChanged(final Color data) {
            // Build and send 'set background color' instruction
            Instruction instruction = new Instruction(this.widget.getWidgetId()) {
                @Override
                protected String getAction() {
                    return "set background color";
                }

                @Override
                protected void fillJsonObject(JsonObject json) {
                    json.addElement("background color", data.toJsonObject());
                }
            };
            this.widget.sendToClient(instruction);
        }
    }
}
