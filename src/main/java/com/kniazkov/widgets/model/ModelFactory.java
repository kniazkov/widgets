/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * A generic factory interface for creating {@link Model} instances.
 * <p>
 * This abstraction is primarily used by higher-order models to lazily produce local (override)
 * models initialized with a given value. It allows widgets and other components to declare
 * how new models of a specific type should be created without hard-coding particular
 * implementations.
 *
 * @param <T> the type of data managed by the model
 */
public interface ModelFactory<T> {

    /**
     * Creates a new {@link Model} initialized with the specified data.
     * Implementations should guarantee that the returned model is in a valid state
     * and reflects the provided value.
     *
     * @param data the initial data value for the created model
     * @return a new model instance containing {@code data}
     */
    Model<T> create(T data);
}
