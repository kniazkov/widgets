/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * Represents a view-layer {@link Entity} that manages a reactive background color model.
 */
public interface HasBgColor extends Entity {

    /**
     * Returns the {@link Model} instance that stores the background color for the specified
     * {@link State}.
     *
     * @param state the logical state whose background color model is requested
     * @return a non-null background color model associated with the given state
     */
    default Model<Color> getBgColorModel(final State state) {
        return this.getModel(state, Property.BG_COLOR);
    }

    /**
     * Associates a new background color model with the specified {@link State}.
     *
     * @param state the logical state to which the model should be assigned
     * @param model the background color model to associate (must not be {@code null})
     */
    default void setBgColorModel(final State state, final Model<Color> model) {
        this.setModel(state, Property.BG_COLOR, model);
    }

    /**
     * Returns the current background color for the specified {@link State}.
     *
     * @param state the logical state whose background color value is requested
     * @return the current background color from the corresponding model
     */
    default Color getBgColor(final State state) {
        return this.getBgColorModel(state).getData();
    }

    /**
     * Returns the background color associated with the default {@link State#NORMAL}.
     *
     * @return the background color for the normal state
     */
    default Color getBgColor() {
        return this.getBgColor(State.NORMAL);
    }

    /**
     * Updates the background color in the model associated with the specified {@link State}.
     *
     * @param state the logical state to update
     * @param color the new background color to set
     */
    default void setBgColor(final State state, final Color color) {
        this.getBgColorModel(state).setData(color);
    }

    /**
     * Updates the background color for all {@link State}s supported by this entity.
     *
     * @param color the new background color to set for every supported state
     */
    default void setBgColor(final Color color) {
        for (final State state : this.getSupportedStates()) {
            this.setBgColor(state, color);
        }
    }
}
