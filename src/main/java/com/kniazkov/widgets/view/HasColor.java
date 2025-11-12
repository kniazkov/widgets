/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * Represents a view-layer {@link Entity} that manages a reactive color model.
 */
public interface HasColor extends Entity {

    /**
     * Returns the {@link Model} instance that stores the color for the specified {@link State}.
     *
     * @param state the logical state whose color model is requested
     * @return a non-null color model associated with the given state
     */
    default Model<Color> getColorModel(final State state) {
        return this.getModel(state, Property.COLOR);
    }

    /**
     * Associates a new color model with the specified {@link State}.
     *
     * @param state the logical state to which the model should be assigned
     * @param model the color model to associate (must not be {@code null})
     */
    default void setColorModel(final State state, final Model<Color> model) {
        this.setModel(state, Property.COLOR, model);
    }

    /**
     * Returns the current color for the specified {@link State}.
     *
     * @param state the logical state whose color value is requested
     * @return the current color from the corresponding model
     */
    default Color getColor(final State state) {
        return this.getColorModel(state).getData();
    }

    /**
     * Returns the color value associated with the default {@link State#NORMAL}.
     *
     * @return the color for the normal state
     */
    default Color getColor() {
        return this.getColor(State.NORMAL);
    }

    /**
     * Updates the color in the model associated with the specified {@link State}.
     *
     * @param state the logical state to update
     * @param color the new color to set
     */
    default void setColor(final State state, final Color color) {
        this.getColorModel(state).setData(color);
    }

    /**
     * Updates the color for all {@link State}s supported by this entity.
     *
     * @param color the new color to set for every supported state
     */
    default void setColor(final Color color) {
        for (final State state : this.getSupportedStates()) {
            this.setColor(state, color);
        }
    }
}
