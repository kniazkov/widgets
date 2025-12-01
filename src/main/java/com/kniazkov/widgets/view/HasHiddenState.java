/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that exposes a reactive boolean model indicating
 * whether the entity is hidden (not visible).
 */
public interface HasHiddenState extends Entity {
    /**
     * Returns the model that stores the hidden-state flag.
     *
     * @return the hidden-state model
     */
    default Model<Boolean> getHiddenStateModel() {
        return this.getModel(State.ANY, Property.HIDDEN);
    }

    /**
     * Sets a new model that stores the hidden-state flag.
     *
     * @param model the hidden-state model to set
     */
    default void setHiddenStateModel(final Model<Boolean> model) {
        this.setModel(State.ANY, Property.HIDDEN, model);
    }

    /**
     * Returns whether this entity is currently hidden.
     *
     * @return {@code true} if the entity is hidden; {@code false} otherwise
     */
    default boolean isHidden() {
        return this.getHiddenStateModel().getData();
    }

    /**
     * Updates the hidden-state flag in the associated model.
     *
     * @param hidden {@code true} to hide the entity; {@code false} to show it
     */
    default void setHiddenFlag(final boolean hidden) {
        this.getHiddenStateModel().setData(hidden);
    }

    /**
     * Hides the entity (makes it invisible).
     */
    default void hide() {
        this.setHiddenFlag(true);
    }

    /**
     * Shows the entity (makes it visible).
     */
    default void show() {
        this.setHiddenFlag(false);
    }
}
