/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} whose background color changes when it gains input focus.
 */
public interface HasFocusBgColor extends HasBgColor {

    /**
     * Returns the model that stores the background color value used when the widget
     * is in the {@link State#ACTIVE} state.
     *
     * @return the focus background color model
     */
    default Model<Color> getFocusBgColorModel() {
        return this.getModel(State.ACTIVE, Property.BG_COLOR);
    }

    /**
     * Sets the model that stores the background color value used when the widget
     * is in the {@link State#ACTIVE} state.
     *
     * @param model the new focus background color model to set
     */
    default void setFocusBgColorModel(final Model<Color> model) {
        this.setModel(State.ACTIVE, Property.BG_COLOR, model);
    }

    /**
     * Returns the current focus background color from the associated model.
     *
     * @return the current focus background color
     */
    default Color getFocusBgColor() {
        return this.getFocusBgColorModel().getData();
    }

    /**
     * Updates the focus background color value in the associated model.
     *
     * @param color the new focus background color to set
     */
    default void setFocusBgColor(final Color color) {
        this.getFocusBgColorModel().setData(color);
    }
}
