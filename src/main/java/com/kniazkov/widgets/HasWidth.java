/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.WidgetSize;

/**
 * An entity that has a configurable width value.
 * <p>
 *     This interface defines a contract for UI components that support a width property,
 *     which is typically expressed as a {@link WidgetSize} and managed through a {@link Model}.
 *     The model enables reactive updates, data binding, and prototype-based inheritance.
 * </p>
 *
 * @param <T> The specific type of {@link WidgetSize} used for this component
 *               (e.g., {@code InlineWidgetSize})
 */
public interface HasWidth<T extends WidgetSize> {

    /**
     * Returns the model that manages the width value of this entity.
     *
     * @return Model representing the width
     */
    Model<T> getWidthModel();

    /**
     * Replaces the width model associated with this entity.
     *
     * @param model New model to manage width
     */
    void setWidthModel(Model<T> model);

    /**
     * Retrieves the current width value.
     * <p>
     *     If not explicitly set, the value may be inherited from a prototype model
     *     or default to an undefined state.
     * </p>
     *
     * @return Current width
     */
    default WidgetSize getWidth() {
        return this.getWidthModel().getData();
    }

    /**
     * Updates the width value directly in the model.
     *
     * @param width New width to apply
     */
    default void setWidth(T width) {
        this.getWidthModel().setData(width);
    }

    /**
     * Listener that tracks changes in an inline widget size model (e.g., width or height)
     * and sends an instruction to the client to update the corresponding CSS property.
     */
    final class WidgetSizeListener<T extends WidgetSize> implements Listener<T> {
        /**
         * Widget associated with the size model.
         */
        private final Widget widget;

        /**
         * Name of the CSS property to update (e.g., "width" or "height").
         */
        private final String name;

        /**
         * Constructs a listener for a given widget and CSS property.
         *
         * @param widget The widget whose size will be updated on the client
         * @param name The name of the CSS property ("width" or "height")
         */
        WidgetSizeListener(final Widget widget, final String name) {
            this.widget = widget;
            this.name = name;
        }

        @Override
        public void dataChanged(final T data) {
            // Build and send 'set width' or 'set height' instruction
            Instruction instruction = new Instruction(widget.getWidgetId()) {
                @Override
                protected String getAction() {
                    return "set " + name;
                }

                @Override
                protected void fillJsonObject(JsonObject json) {
                    json.addString(name, data.getCSSCode());
                }
            };
            widget.sendToClient(instruction);
        }
    }
}
