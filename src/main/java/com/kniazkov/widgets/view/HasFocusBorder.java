/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} whose border color changes when it gains input focus.
 * <p>
 * This interface extends {@link HasBorder} and provides access to the border color model
 * for the {@link State#ACTIVE} state.
 */
public interface HasFocusBorder extends HasBorder {

    /**
     * Returns the model that stores the border color used when the widget is in the
     * {@link State#ACTIVE} state.
     *
     * @return the focus border color model
     */
    default Model<Color> getFocusBorderColorModel() {
        return this.getModel(State.ACTIVE, Property.BORDER_COLOR);
    }

    /**
     * Sets the model that stores the border color used when the widget is in the
     * {@link State#ACTIVE} state.
     *
     * @param model the new focus border color model
     */
    default void setFocusBorderColorModel(final Model<Color> model) {
        this.setModel(State.ACTIVE, Property.BORDER_COLOR, model);
    }

    /**
     * Returns the current focus border color from the associated model.
     *
     * @return the current focus border color
     */
    default Color getFocusBorderColor() {
        return this.getFocusBorderColorModel().getData();
    }

    /**
     * Updates the focus border color value in the associated model.
     *
     * @param color the new focus border color
     */
    default void setFocusBorderColor(final Color color) {
        this.getFocusBorderColorModel().setData(color);
    }
}
