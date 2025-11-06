/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} whose background color changes when it becomes disabled.
 */
public interface HasDisabledBgColor extends HasBgColor {

    /**
     * Returns the model that stores the background color value used when the widget
     * is in the {@link State#DISABLED} state.
     *
     * @return the disabled background color model
     */
    default Model<Color> getDisabledBgColorModel() {
        return this.getModel(State.DISABLED, Property.BG_COLOR);
    }

    /**
     * Sets the model that stores the background color value used when the widget
     * is in the {@link State#DISABLED} state.
     *
     * @param model the new disabled background color model to set
     */
    default void setDisabledBgColorModel(final Model<Color> model) {
        this.setModel(State.DISABLED, Property.BG_COLOR, model);
    }

    /**
     * Returns the current disabled background color from the associated model.
     *
     * @return the current disabled background color
     */
    default Color getDisabledBgColor() {
        return this.getDisabledBgColorModel().getData();
    }

    /**
     * Updates the disabled background color value in the associated model.
     *
     * @param color the new disabled background color to set
     */
    default void setDisabledBgColor(final Color color) {
        this.getDisabledBgColorModel().setData(color);
    }
}
