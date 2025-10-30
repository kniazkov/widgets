/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} whose color changes when its validation state is invalid.
 */
public interface HasInvalidColor extends HasColor {

    /**
     * Returns the model that stores the color value used when the widget is in the
     * {@link State#INVALID} state.
     *
     * @return the invalid color model
     */
    default Model<Color> getInvalidColorModel() {
        return this.getModel(State.INVALID, Property.COLOR);
    }

    /**
     * Sets the model that stores the color value used when the widget is in the
     * {@link State#INVALID} state.
     *
     * @param model the new invalid color model to set
     */
    default void setInvalidColorModel(final Model<Color> model) {
        this.setModel(State.INVALID, Property.COLOR, model);
    }

    /**
     * Returns the current invalid color from the associated model.
     *
     * @return the current invalid color
     */
    default Color getInvalidColor() {
        return this.getInvalidColorModel().getData();
    }

    /**
     * Updates the invalid color value in the associated model.
     *
     * @param color the new invalid color to set
     */
    default void setInvalidColor(final Color color) {
        this.getInvalidColorModel().setData(color);
    }
}