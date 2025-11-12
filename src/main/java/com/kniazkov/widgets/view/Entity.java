/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;
import java.util.Collections;
import java.util.Set;

/**
 * Represents a view-layer entity that exposes reactive {@link Model} instances associated with
 * specific {@link State} and {@link Property} keys.
 * Implementations of this interface must guarantee that operations always succeed for valid
 * arguments. If a requested model cannot be found or assigned due to invalid parameters,
 * an {@link IllegalArgumentException} must be thrown.
 */
public interface Entity {
    /**
     * Returns the set of supported logical states for this entity.
     * <p>
     * Implementations may override this method to declare which states they actually support.
     * The default implementation returns an empty, immutable set.
     *
     * @return the set of supported states (never {@code null})
     */
    default Set<State> getSupportedStates() {
        return Collections.emptySet();
    }

    /**
     * Returns the model associated with the specified {@link State} and {@link Property}.
     * <p>
     * Implementations must always return a non-null model of the requested type.
     * If no model exists or its data type does not match the expected {@code type},
     * this method must throw an {@link IllegalArgumentException}.
     *
     * @param state the logical state (e.g. normal, hovered, disabled)
     * @param property the visual or behavioral property
     * @param <T> the type of data managed by the model
     * @return the model for the specified state/property pair (never {@code null})
     * @throws IllegalArgumentException if the type does not match the property or the state is
     *  not supported
     */
    <T> Model<T> getModel(State state, Property<T> property);

    /**
     * Associates the specified model with the given {@link State} and {@link Property}.
     * <p>
     * Implementations must guarantee that the model is registered successfully
     * for the specified type. If the provided arguments are invalid (for example,
     * unsupported state, property, or mismatched data type),
     * this method must throw an {@link IllegalArgumentException}.
     * <p>
     * The supplied model must not be {@code null}.
     *
     * @param state the logical state (e.g. normal, hovered, disabled)
     * @param property the visual or behavioral property
     * @param model the model to associate (must not be {@code null})
     * @param <T> the type of data managed by the model
     * @throws IllegalArgumentException if the type does not match the property or the state is
     *  not supported
     */
    <T> void setModel(State state, Property<T> property, Model<T> model);
}
