/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} whose color changes when it becomes disabled.
 */
public interface HasDisabledColor extends HasColor {

    /**
     * Returns the model that stores the color value used when the widget is in the
     * {@link State#DISABLED} state.
     *
     * @return the disabled color model
     */
    default Model<Color> getDisabledColorModel() {
        return this.getModel(State.DISABLED, Property.COLOR);
    }

    /**
     * Sets the model that stores the color value used when the widget is in the
     * {@link State#DISABLED} state.
     *
     * @param model the new disabled color model to set
     */
    default void setDisabledColorModel(final Model<Color> model) {
        this.setModel(State.DISABLED, Property.COLOR, model);
    }

    /**
     * Returns the current disabled color from the associated model.
     *
     * @return the current disabled color
     */
    default Color getDisabledColor() {
        return this.getDisabledColorModel().getData();
    }

    /**
     * Updates the disabled color value in the associated model.
     *
     * @param color the new disabled color to set
     */
    default void setDisabledColor(final Color color) {
        this.getDisabledColorModel().setData(color);
    }
}
