/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * An object that stores and processes data.
 * The first component of the Model-View-Controller triad.
 * @param <T> Type of model data
 */
public abstract class Model<T> {
    /**
     * Set of listeners.
     */
    private final Set<ModelListener<T>> listeners;

    /**
     * Constructor.
     */
    public Model() {
        this.listeners = new HashSet<>();
    }

    /**
     * Returns the data that the model stores.
     * @return Data
     */
    public abstract @NotNull T getData();

    /**
     * Checks the new data. If the data is correct, writes it.
     * @param data New data
     * @return Validation result
     */
    protected abstract boolean writeData(@NotNull T data);

    /**
     * Sets up new data. If the new data is valid and different from the old data,
     * all model listeners are notified.
     * @param data New data
     */
    public void setData(@NotNull T data) {
        if (!this.getData().equals(data)) {
            boolean notify = writeData(data);
            if (notify) {
                for (final ModelListener<T> listener : listeners) {
                    listener.dataChanged(data);
                }
            }
        }
    }

    /**
     * Checks whether the model is in a valid state
     * (i.e., contains correct data relative to the model logic).
     * @return Checking result
     */
    public abstract boolean isValid();

    /**
     * Adds a listener to the model. Listeners are notified each time model data is updated.
     * @param listener Listener
     */
    public void addListener(@NotNull ModelListener<T> listener) {
        this.listeners.add(listener);
    }

    /**
     * Removes the listener from the model.
     * @param listener Listener
     */
    public void removeListener(@NotNull ModelListener<T> listener) {
        this.listeners.remove(listener);
    }
}
