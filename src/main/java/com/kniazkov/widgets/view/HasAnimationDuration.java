/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;
import java.util.Objects;

/**
 * An entity with a reactive nonnegative duration in milliseconds.
 */
public interface HasAnimationDuration extends Entity {
    /**
     * Returns the property model shared by the standard style and widget binding machinery.
     * @return property model
     */
    default Model<Integer> getAnimationDurationModel() {
        return this.getModel(State.ANY, Property.ANIMATION_DURATION);
    }

    /**
     * Replaces the property model. Future values must remain nonnegative.
     * @param model replacement model
     */
    default void setAnimationDurationModel(final Model<Integer> model) {
        final int value = Objects.requireNonNull(model, "model").getData();
        if (value < 0) {
            throw new IllegalArgumentException("Expected a nonnegative duration in milliseconds");
        }
        this.setModel(State.ANY, Property.ANIMATION_DURATION, model);
    }

    /**
     * Returns the current value.
     * @return nonnegative duration in milliseconds; defaults to 250
     */
    default int getAnimationDuration() {
        return this.getAnimationDurationModel().getData();
    }

    /**
     * Updates the property model.
     * @param value nonnegative duration in milliseconds
     */
    default void setAnimationDuration(final int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Expected a nonnegative duration in milliseconds");
        }
        this.getAnimationDurationModel().setData(value);
    }
}
