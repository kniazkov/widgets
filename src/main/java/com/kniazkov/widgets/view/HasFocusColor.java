/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} whose color changes when it gains input focus.
 */
public interface HasFocusColor extends HasColor {
    /**
     * Returns the model that stores the color value used when the widget is in the
     * {@link State#ACTIVE} state.
     *
     * @return the focus color model
     */
    default Model<Color> getFocusColorModel() {
        return this.getModel(State.ACTIVE, Property.COLOR);
    }

    /**
     * Sets the model that stores the color value used when the widget is in the
     * {@link State#ACTIVE} state.
     *
     * @param model the new focus color model to set
     */
    default void setFocusColorModel(final Model<Color> model) {
        this.setModel(State.ACTIVE, Property.COLOR, model);
    }

    /**
     * Returns the current focus color from the associated model.
     *
     * @return the current focus color
     */
    default Color getFocusColor() {
        return this.getFocusColorModel().getData();
    }

    /**
     * Updates the focus color value in the associated model.
     *
     * @param color the new focus color to set
     */
    default void setFocusColor(final Color color) {
        this.getFocusColorModel().setData(color);
    }
}
