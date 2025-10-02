/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Base class for a data model in an MVC architecture.
 * The model is responsible for holding, validating, and persisting application data.
 * It defines a contract for reading, writing, and providing default values for the data,
 * while also supporting listener registration to notify other components (typically controllers
 * or views) about changes.
 *
 * @param <T> the type of the data managed by this model
 */
public abstract class Model<T> {
    /**
     * The set of registered listeners.
     */
    private final Set<Listener<T>> listeners = new HashSet<>();

    /**
     * Checks whether the model currently holds valid data.
     * Even if the model is not valid, data can still be read using {@link #getData()},
     * but in that case the default value will be returned.
     * This guarantees that clients always receive a valid, usable object.
     *
     * @return true if the model has valid data, false otherwise
     */
    public abstract boolean isValid();

    /**
     * Reads the current data from the underlying source.
     *
     * @return an {@link Optional} containing the data if available
     */
    protected abstract Optional<T> readData();

    /**
     * Provides a default value for the model data.
     * The default is returned whenever the model is invalid or cannot provide a meaningful value.
     *
     * @return the default data
     */
    protected abstract T getDefaultData();

    /**
     * Writes new data to the underlying source.
     * Implementations must guarantee that if writing fails (returns {@code false}),
     * the existing model data remains intact and is not corrupted.
     *
     * @param data the new data to write
     * @return true if the write was successful, false otherwise
     */
    protected abstract boolean writeData(T data);

    /**
     * Retrieves the current data.
     * If the model is valid and readable, returns the stored data;
     * otherwise returns the default data.
     * This method guarantees that the returned value is always valid and never {@code null}.
     *
     * @return the current or default data
     */
    public T getData() {
        if (this.isValid()) {
            Optional<T> data = this.readData();
            if (data.isPresent()) {
                return data.get();
            }
        }
        return this.getDefaultData();
    }

    /**
     * Updates the model data if it differs from the current one,
     * and notifies listeners if the update succeeds.
     *
     * @param data the new data to set
     */
    public void setData(final T data) {
        final Optional<T> oldData = this.readData();
        if (!Objects.equals(oldData.orElse(this.getDefaultData()), data)
                && this.writeData(data)) {
            this.notifyListeners(data);
        }
    }

    /**
     * Registers a new listener.
     *
     * @param listener the listener to add
     */
    public void addListener(final Listener<T> listener) {
        this.listeners.add(listener);
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(final Listener<T> listener) {
        this.listeners.remove(listener);
    }

    /**
     * Notifies all registered listeners with the current data.
     */
    public void notifyListeners() {
        this.notifyListeners(this.getData());
    }

    /**
     * Notifies all registered listeners with the provided data.
     *
     * @param data the data to send to listeners
     */
    protected void notifyListeners(final T data) {
        for (final Listener<T> listener : this.listeners) {
            listener.accept(data);
        }
    }
}
