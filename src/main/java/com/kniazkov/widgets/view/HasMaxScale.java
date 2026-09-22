/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;
import java.util.Objects;

/**
 * An entity with a reactive finite scale of at least 1.
 */
public interface HasMaxScale extends Entity {
    /**
     * Returns the property model shared by the standard style and widget binding machinery.
     * @return property model
     */
    default Model<Double> getMaxScaleModel() {
        return this.getModel(State.ANY, Property.MAX_SCALE);
    }

    /**
     * Replaces the property model. Future values must remain a finite scale of at least 1.
     * @param model replacement model
     */
    default void setMaxScaleModel(final Model<Double> model) {
        final double value = Objects.requireNonNull(model, "model").getData();
        if (!Double.isFinite(value) || value < 1) {
            throw new IllegalArgumentException("Expected a finite scale of at least 1");
        }
        this.setModel(State.ANY, Property.MAX_SCALE, model);
    }

    /**
     * Returns the current value.
     * @return finite scale of at least 1; defaults to 8
     */
    default double getMaxScale() {
        return this.getMaxScaleModel().getData();
    }

    /**
     * Updates the property model.
     * @param value finite scale of at least 1
     */
    default void setMaxScale(final double value) {
        if (!Double.isFinite(value) || value < 1) {
            throw new IllegalArgumentException("Expected a finite scale of at least 1");
        }
        this.getMaxScaleModel().setData(value);
    }
}
