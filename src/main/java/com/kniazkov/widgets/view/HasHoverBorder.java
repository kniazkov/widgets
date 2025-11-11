/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} whose border color changes when the pointer hovers over it.
 * <p>
 * This interface extends {@link HasBorder} and provides access to the border color model
 * for the {@link State#HOVERED} state.
 */
public interface HasHoverBorder extends HasBorder {

    /**
     * Returns the model that stores the border color used when the widget is in the
     * {@link State#HOVERED} state.
     *
     * @return the hover border color model
     */
    default Model<Color> getHoverBorderColorModel() {
        return this.getModel(State.HOVERED, Property.BORDER_COLOR);
    }

    /**
     * Sets the model that stores the border color used when the widget is in the
     * {@link State#HOVERED} state.
     *
     * @param model the new hover border color model
     */
    default void setHoverBorderColorModel(final Model<Color> model) {
        this.setModel(State.HOVERED, Property.BORDER_COLOR, model);
    }

    /**
     * Returns the current hover border color from the associated model.
     *
     * @return the current hover border color
     */
    default Color getHoverBorderColor() {
        return this.getHoverBorderColorModel().getData();
    }

    /**
     * Updates the hover border color value in the associated model.
     *
     * @param color the new hover border color
     */
    default void setHoverBorderColor(final Color color) {
        this.getHoverBorderColorModel().setData(color);
    }
}
