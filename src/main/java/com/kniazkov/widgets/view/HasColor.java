/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * A {@link View} that has an associated color model.
 */
public interface HasColor extends Entity {
    /**
     * Returns the model that stores the color for this view.
     *
     * @return the color model
     */
    default Model<Color> getColorModel() {
        return this.getModel(State.NORMAL, Property.COLOR, Color.class);
    }

    /**
     * Sets a new color model for this view.
     *
     * @param model the color model to set
     */
    default void setColorModel(final Model<Color> model) {
        this.setModel(State.NORMAL, Property.COLOR, Color.class, model);
    }

    /**
     * Returns the current color from the current model.
     *
     * @return the current color
     */
    default Color getColor() {
        return this.getColorModel().getData();
    }

    /**
     * Updates the color in the model.
     *
     * @param color the new color
     */
    default void setColor(final Color color) {
        this.getColorModel().setData(color);
    }
}
