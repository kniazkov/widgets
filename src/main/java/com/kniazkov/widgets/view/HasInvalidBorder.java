/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} whose border color changes when its validation state becomes invalid.
 * <p>
 * This interface extends {@link HasBorder} and provides access to the border color model
 * for the {@link State#INVALID} state.
 */
public interface HasInvalidBorder extends HasBorder {

    /**
     * Returns the model that stores the border color used when the widget is in the
     * {@link State#INVALID} state.
     *
     * @return the invalid border color model
     */
    default Model<Color> getInvalidBorderColorModel() {
        return this.getModel(State.INVALID, Property.BORDER_COLOR);
    }

    /**
     * Sets the model that stores the border color used when the widget is in the
     * {@link State#INVALID} state.
     *
     * @param model the new invalid border color model
     */
    default void setInvalidBorderColorModel(final Model<Color> model) {
        this.setModel(State.INVALID, Property.BORDER_COLOR, model);
    }

    /**
     * Returns the current invalid border color from the associated model.
     *
     * @return the current invalid border color
     */
    default Color getInvalidBorderColor() {
        return this.getInvalidBorderColorModel().getData();
    }

    /**
     * Updates the invalid border color value in the associated model.
     *
     * @param color the new invalid border color
     */
    default void setInvalidBorderColor(final Color color) {
        this.getInvalidBorderColorModel().setData(color);
    }
}
