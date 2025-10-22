/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * Default in-memory implementation of {@link Model}.
 * This class provides a simple and reliable data model suitable for most use cases
 * where the model only needs to store data in memory and does not require
 * validation, persistence, or complex synchronization logic.
 *
 * @param <T> the type of the data managed by this model
 */
public abstract class DefaultModel<T> extends SingleThreadModel<T> {
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
    public T getData() {
        return this.data;
    }

    @Override
    public boolean setData(final T data) {
        if (this.data.equals(data)) {
            return false;
        }
        this.data = data;
        this.notifyListeners(data);
        return true;
    }

    /**
     * Provides the default value for model data. The “no data” model is filled with this data.
     * Implementations must ensure that this method never returns {@code null}.
     *
     * @return the default data value
     */
    protected abstract T getDefaultData();
}
