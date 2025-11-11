/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} whose border color changes when it becomes disabled.
 * <p>
 * This interface extends {@link HasBorder} and provides access to the border color model
 * for the {@link State#DISABLED} state.
 */
public interface HasDisabledBorder extends HasBorder {

    /**
     * Returns the model that stores the border color used when the widget is in the
     * {@link State#DISABLED} state.
     *
     * @return the disabled border color model
     */
    default Model<Color> getDisabledBorderColorModel() {
        return this.getModel(State.DISABLED, Property.BORDER_COLOR);
    }

    /**
     * Sets the model that stores the border color used when the widget is in the
     * {@link State#DISABLED} state.
     *
     * @param model the new disabled border color model
     */
    default void setDisabledBorderColorModel(final Model<Color> model) {
        this.setModel(State.DISABLED, Property.BORDER_COLOR, model);
    }

    /**
     * Returns the current disabled border color from the associated model.
     *
     * @return the current disabled border color
     */
    default Color getDisabledBorderColor() {
        return this.getDisabledBorderColorModel().getData();
    }

    /**
     * Updates the disabled border color value in the associated model.
     *
     * @param color the new disabled border color
     */
    default void setDisabledBorderColor(final Color color) {
        this.getDisabledBorderColorModel().setData(color);
    }
}
