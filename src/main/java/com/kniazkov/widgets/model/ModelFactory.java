/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * A generic factory interface for creating {@link Model} instances.
 * This abstraction is primarily used by higher-order models to lazily produce local (override)
 * models initialized with a given value. It allows widgets and other components to declare
 * how new models of a specific type should be created without hard-coding particular
 * implementations.
 *
 * @param <T> the type of data managed by the model
 */
public interface ModelFactory<T> {
    /**
     * Creates a new, empty {@link Model} instance with default data.
     * The returned model must be valid and usable immediately after creation.
     * Its initial content is defined by the model’s {@link Model#getDefaultData()}.
     *
     * @return a new model instance containing its default data
     */
    Model<T> create();

    /**
     * Creates a new {@link Model} initialized with the specified data.
     *
     * @param data the initial data value for the created model
     * @return a new model instance containing {@code data}
     */
    default Model<T> create(final T data) {
        Model<T> model = this.create();
        model.setData(data);
        return model;
    }
}
