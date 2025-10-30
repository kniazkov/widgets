/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} whose color changes when the pointer hovers over it.
 */
public interface HasHoverColor extends HasColor {
    /**
     * Returns the model that stores the color value used when the widget is in the
     * {@link State#HOVERED} state.
     *
     * @return the hover color model
     */
    default Model<Color> getHoverColorModel() {
        return this.getModel(State.HOVERED, Property.COLOR);
    }

    /**
     * Sets the model that stores the color value used when the widget is in the
     * {@link State#HOVERED} state.
     *
     * @param model the new hover color model to set
     */
    default void setHoverColorModel(final Model<Color> model) {
        this.setModel(State.HOVERED, Property.COLOR, model);
    }

    /**
     * Returns the current hover color from the associated model.
     *
     * @return the current hover color
     */
    default Color getHoverColor() {
        return this.getHoverColorModel().getData();
    }

    /**
     * Updates the hover color value in the associated model.
     *
     * @param color the new hover color to set
     */
    default void setHoverColor(final Color color) {
        this.getHoverColorModel().setData(color);
    }
}
