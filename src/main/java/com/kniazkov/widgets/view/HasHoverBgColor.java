/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} whose background color changes when the pointer hovers over it.
 */
public interface HasHoverBgColor extends HasBgColor {

    /**
     * Returns the model that stores the background color value used when the widget
     * is in the {@link State#HOVERED} state.
     *
     * @return the hover background color model
     */
    default Model<Color> getHoverBgColorModel() {
        return this.getModel(State.HOVERED, Property.BG_COLOR);
    }

    /**
     * Sets the model that stores the background color value used when the widget
     * is in the {@link State#HOVERED} state.
     *
     * @param model the new hover background color model to set
     */
    default void setHoverBgColorModel(final Model<Color> model) {
        this.setModel(State.HOVERED, Property.BG_COLOR, model);
    }

    /**
     * Returns the current hover background color from the associated model.
     *
     * @return the current hover background color
     */
    default Color getHoverBgColor() {
        return this.getHoverBgColorModel().getData();
    }

    /**
     * Updates the hover background color value in the associated model.
     *
     * @param color the new hover background color to set
     */
    default void setHoverBgColor(final Color color) {
        this.getHoverBgColorModel().setData(color);
    }
}
