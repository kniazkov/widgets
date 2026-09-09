/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * An entity with a reactive color for the backdrop behind a modal popup.
 */
public interface HasBackdropColor extends Entity {
    /**
     * Returns the backdrop color model.
     *
     * @return backdrop color model
     */
    default Model<Color> getBackdropColorModel() {
        return this.getModel(State.ANY, Property.BACKDROP_COLOR);
    }

    /**
     * Replaces the backdrop color model.
     *
     * @param model backdrop color model
     */
    default void setBackdropColorModel(final Model<Color> model) {
        this.setModel(State.ANY, Property.BACKDROP_COLOR, model);
    }

    /**
     * Returns the current backdrop color.
     *
     * @return backdrop color
     */
    default Color getBackdropColor() {
        return this.getBackdropColorModel().getData();
    }

    /**
     * Updates the backdrop color.
     *
     * @param color backdrop color
     */
    default void setBackdropColor(final Color color) {
        this.getBackdropColorModel().setData(color);
    }
}
