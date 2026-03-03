/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * Represents a view-layer {@link Entity} that manages a reactive opacity model.
 * <p>
 * Opacity values are expected to be in the range [0.0, 1.0], where 0.0 is fully
 * transparent and 1.0 is fully opaque.
 */
public interface HasOpacity extends Entity {

    /**
     * Returns the {@link Model} instance that stores the opacity for the specified {@link State}.
     *
     * @param state the logical state whose opacity model is requested
     * @return a non-null opacity model associated with the given state
     */
    default Model<Double> getOpacityModel(final State state) {
        return this.getModel(state, Property.OPACITY);
    }

    /**
     * Associates a new opacity model with the specified {@link State}.
     *
     * @param state the logical state to which the model should be assigned
     * @param model the opacity model to associate
     */
    default void setOpacityModel(final State state, final Model<Double> model) {
        this.setModel(state, Property.OPACITY, model);
    }

    /**
     * Returns the current opacity for the specified {@link State}.
     *
     * @param state the logical state whose opacity value is requested
     * @return the current opacity from the corresponding model
     */
    default double getOpacity(final State state) {
        return this.getOpacityModel(state).getData();
    }

    /**
     * Returns the opacity value associated with the default {@link State#NORMAL}.
     *
     * @return the opacity for the normal state
     */
    default double getOpacity() {
        return this.getOpacity(State.NORMAL);
    }

    /**
     * Updates the opacity in the model associated with the specified {@link State}.
     *
     * @param state the logical state to update
     * @param opacity the new opacity to set (should be in range [0.0, 1.0])
     */
    default void setOpacity(final State state, final double opacity) {
        this.getOpacityModel(state).setData(opacity);
    }

    /**
     * Updates the opacity for all {@link State}s supported by this entity.
     *
     * @param opacity the new opacity to set for every supported state
     */
    default void setOpacity(final double opacity) {
        for (final State state : this.getSupportedStates()) {
            this.setOpacity(state, opacity);
        }
    }
}
