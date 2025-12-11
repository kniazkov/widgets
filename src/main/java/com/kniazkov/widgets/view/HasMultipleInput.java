/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that exposes a reactive boolean model indicating
 * whether the entity accepts multiple input values.
 * <p>
 * The multiple input flag typically controls whether the entity can accept
 * multiple values simultaneously (e.g., file inputs allowing multiple file selection,
 * list boxes with multi-select capability, or tag inputs).
 */
public interface HasMultipleInput extends Entity {
    /**
     * Returns the model that stores the multiple-input flag.
     *
     * @return the multiple-input model
     */
    default Model<Boolean> getMultipleInputModel() {
        return this.getModel(State.ANY, Property.MULTIPLE_INPUT);
    }

    /**
     * Sets a new model that stores the multiple-input flag.
     *
     * @param model the multiple-input model to set
     */
    default void setMultipleInputModel(final Model<Boolean> model) {
        this.setModel(State.ANY, Property.MULTIPLE_INPUT, model);
    }

    /**
     * Returns whether this entity currently accepts multiple input values.
     *
     * @return {@code true} if multiple input is enabled; {@code false} otherwise
     */
    default boolean hasMultipleInput() {
        return this.getMultipleInputModel().getData();
    }

    /**
     * Updates the multiple-input flag in the associated model.
     *
     * @param multiple {@code true} to enable multiple input; {@code false} to disable it
     */
    default void setMultipleInputFlag(final boolean multiple) {
        this.getMultipleInputModel().setData(multiple);
    }
}
