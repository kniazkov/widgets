/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that exposes a reactive boolean model indicating
 * whether the entity is in a valid state.
 * <p>
 * This flag typically affects how the entity is styled (e.g. error colors).
 */
public interface HasInvalidState extends Entity {
    /**
     * Returns the model that stores the valid-state flag.
     *
     * @return the valid-state model
     */
    default Model<Boolean> getValidStateModel() {
        return this.getModel(State.ANY, Property.VALID);
    }

    /**
     * Sets a new model that stores the valid-state flag.
     *
     * @param model the valid-state model to set
     */
    default void setValidStateModel(final Model<Boolean> model) {
        this.setModel(State.ANY, Property.VALID, model);
    }

    /**
     * Returns whether this entity is currently marked as valid.
     *
     * @return {@code true} if the entity is invalid; {@code false} otherwise
     */
    default boolean isValid() {
        return this.getValidStateModel().getData();
    }

    /**
     * Updates the valid-state flag in the associated model.
     *
     * @param state {@code true} to mark the entity as valid; {@code false} as invalid
     */
    default void setValidState(final boolean state) {
        this.getValidStateModel().setData(state);
    }
}
