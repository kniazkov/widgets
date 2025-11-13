/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that exposes a reactive boolean model indicating
 * whether the entity is disabled.
 * <p>
 * The disabled flag typically affects both the entity's style and its behavior
 * (e.g. ignoring user interaction).
 */
public interface HasDisabledState extends Entity {
    /**
     * Returns the model that stores the disabled-state flag.
     *
     * @return the disabled-state model
     */
    default Model<Boolean> getDisabledStateModel() {
        return this.getModel(State.ANY, Property.DISABLED);
    }

    /**
     * Sets a new model that stores the disabled-state flag.
     *
     * @param model the disabled-state model to set
     */
    default void setDisabledStateModel(final Model<Boolean> model) {
        this.setModel(State.ANY, Property.DISABLED, model);
    }

    /**
     * Returns whether this entity is currently disabled.
     *
     * @return {@code true} if the entity is disabled; {@code false} otherwise
     */
    default boolean isDisabled() {
        return this.getDisabledStateModel().getData();
    }

    /**
     * Updates the disabled-state flag in the associated model.
     *
     * @param disabled {@code true} to disable the entity; {@code false} to enable it
     */
    default void setDisabledFlag(final boolean disabled) {
        this.getDisabledStateModel().setData(disabled);
    }

    /**
     * Disables the entity.
     */
    default void disable() {
        this.setDisabledFlag(true);
    }

    /**
     * Enables the entity.
     */
    default void enable() {
        this.setDisabledFlag(false);
    }
}
