/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} whose background color changes when its validation state is invalid.
 */
public interface HasInvalidBgColor extends HasBgColor {

    /**
     * Returns the model that stores the background color value used when the widget
     * is in the {@link State#INVALID} state.
     *
     * @return the invalid background color model
     */
    default Model<Color> getInvalidBgColorModel() {
        return this.getModel(State.INVALID, Property.BG_COLOR);
    }

    /**
     * Sets the model that stores the background color value used when the widget
     * is in the {@link State#INVALID} state.
     *
     * @param model the new invalid background color model to set
     */
    default void setInvalidBgColorModel(final Model<Color> model) {
        this.setModel(State.INVALID, Property.BG_COLOR, model);
    }

    /**
     * Returns the current invalid background color from the associated model.
     *
     * @return the current invalid background color
     */
    default Color getInvalidBgColor() {
        return this.getInvalidBgColorModel().getData();
    }

    /**
     * Updates the invalid background color value in the associated model.
     *
     * @param color the new invalid background color to set
     */
    default void setInvalidBgColor(final Color color) {
        this.getInvalidBgColorModel().setData(color);
    }
}
