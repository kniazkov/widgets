/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * An object that stores and processes data.
 * The first component of the Model-View-Controller triad.
 * @param <T> Type of model data
 */
public interface Model<T> {
    /**
     * Returns the data that the model stores.
     * @return Data
     */
    @NotNull T getData();

    /**
     * Sets up new data. If the new data is valid and different from the old data,
     * all model listeners are notified.
     * @param data New data
     */
    void setData(@NotNull T data);

    /**
     * Checks whether the model is in a valid state
     * (i.e., contains correct data relative to the model logic).
     * @return Checking result
     */
    boolean isValid();

    /**
     * Adds a listener to the model. Listeners are notified each time model data is updated.
     * @param listener Listener
     */
    void addListener(@NotNull ModelListener<T> listener);

    /**
     * Removes the listener from the model.
     * @param listener Listener
     */
    void removeListener(@NotNull ModelListener<T> listener);
}
