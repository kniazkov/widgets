/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that exposes a reactive boolean model indicating
 * whether the entity is checked/selected.
 * <p>
 * The checked flag is commonly used for toggle controls like checkboxes, radio buttons,
 * or toggle switches to indicate selection state.
 */
public interface HasCheckedState extends Entity {
    /**
     * Returns the model that stores the checked-state flag.
     *
     * @return the checked-state model
     */
    default Model<Boolean> getCheckedStateModel() {
        return this.getModel(State.ANY, Property.CHECKED);
    }

    /**
     * Sets a new model that stores the checked-state flag.
     *
     * @param model the checked-state model to set
     */
    default void setCheckedStateModel(final Model<Boolean> model) {
        this.setModel(State.ANY, Property.CHECKED, model);
    }

    /**
     * Returns whether this entity is currently checked.
     *
     * @return {@code true} if the entity is checked; {@code false} otherwise
     */
    default boolean isChecked() {
        return this.getCheckedStateModel().getData();
    }

    /**
     * Updates the checked-state flag in the associated model.
     *
     * @param checked {@code true} to check the entity; {@code false} to uncheck it
     */
    default void setCheckedFlag(final boolean checked) {
        this.getCheckedStateModel().setData(checked);
    }

    /**
     * Checks the entity (sets the checked state to {@code true}).
     */
    default void check() {
        this.setCheckedFlag(true);
    }

    /**
     * Unchecks the entity (sets the checked state to {@code false}).
     */
    default void uncheck() {
        this.setCheckedFlag(false);
    }

    /**
     * Toggles the checked state of the entity.
     * If currently checked, unchecks it; if currently unchecked, checks it.
     */
    default void toggle() {
        this.setCheckedFlag(!this.isChecked());
    }
}
