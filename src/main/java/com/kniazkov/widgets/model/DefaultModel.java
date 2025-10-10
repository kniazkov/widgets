/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Default in-memory implementation of {@link Model}.
 * This model always contains valid data. The internal value is initialized with
 * {@link #getDefaultData()} at construction time.
 *
 * @param <T> the type of the data managed by this model
 */
public abstract class DefaultModel<T> extends Model<T> {
    /**
     * The internal data storage.
     */
    private T data;

    /**
     * Creates a new default model instance.
     */
    protected DefaultModel() {
        this.data = this.getDefaultData();
    }

    /**
     * Creates a new default model initialized with the specified value.
     * This value immediately becomes the current and default data of the model.
     *
     * @param data the initial value to store in the model
     */
    protected DefaultModel(T data) {
        this.data = data;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    protected Optional<T> readData() {
        return Optional.of(this.data);
    }

    @Override
    protected boolean writeData(final T data) {
        this.data = Objects.requireNonNull(data);
        return true;
    }
}
