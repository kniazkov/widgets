/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Color;

/**
 * An entity that has an associated color.
 * <p>
 *     This interface abstracts the concept of color ownership, allowing components
 *     (such as widgets, shapes, or other UI elements) to expose and modify their color
 *     via a {@link Model}{@code <Color>}.
 * </p>
 */
public interface HasColor {
    /**
     * Returns the model responsible for holding and processing the color of this entity.
     *
     * @return Model containing the current color
     */
    Model<Color> getColorModel();

    /**
     * Sets the model responsible for storing and processing the color of this entity.
     *
     * @param model Model that will represent the color
     */
    void setColorModel(Model<Color> model);

    /**
     * Returns the current color from the color model.
     * <p>
     *     This method delegates to {@link Model#getData()} and always returns
     *     a valid, non-null color.
     * </p>
     *
     * @return The current color
     */
    default Color getColor() {
        return this.getColorModel().getData();
    }

    /**
     * Sets a new color value in the color model.
     * <p>
     *     This method delegates to {@link Model#setData(Object)}.
     * </p>
     *
     * @param color New color to assign
     */
    default void setColor(Color color) {
        this.getColorModel().setData(color);
    }

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
}
