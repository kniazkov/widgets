/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Standard model, which is used by default and is suitable for most cases.
 * It does not contain any logic to validate the data, any data is considered valid.
 * It simply stores data of the specified type and notifies listeners when that data changes.
 * @param <T> Type of model data
 */
public abstract class DefaultModel<T> implements Model<T> {
    /**
     * Data.
     */
    private T data;

    /**
     * Set of listeners.
     */
    private final Set<ModelListener<T>> listeners;

    /**
     * Constructor.
     */
    public DefaultModel() {
        data = getDefaultData();
        listeners = new HashSet<>();
    }

    @Override
    public @NotNull T getData() {
        return data;
    }

    @Override
    public void setData(final @NotNull T data) {
        Objects.requireNonNull(data);
        final boolean notify = !data.equals(this.data);
        this.data = data;
        if (notify) {
            for (ModelListener<T> listener : listeners) {
                listener.dataChanged(data);
            }
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addListener(final @NotNull ModelListener<T> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(final @NotNull ModelListener<T> listener) {
        listeners.remove(listener);
    }

    /**
     * Returns the default data that the model contains after creation and before any changes.
     * @return Default data.
     */
    public abstract @NotNull T getDefaultData();
}
