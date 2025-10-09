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
 * A {@code Model} encapsulates a unit of application data, providing a uniform interface
 * for reading, writing, and validation. Models are designed to be reactive: components such as
 * views or controllers can register listeners to be notified when the data changes.
 * The model guarantees that {@link #getData()} always returns a usable value —
 * either the last successfully read data or a default fallback.
 * The validity of the data can be checked separately via {@link #isValid()}.
 * Models are not thread-safe by default.
 *
 * @param <T> the type of the data managed by this model
 */
public abstract class Model<T> {

    /**
     * The set of registered listeners.
     * Listeners are notified whenever the model's data is updated
     * through {@link #setData(Object)} or when {@link #notifyListeners()} is called explicitly.
     */
    private final Set<Listener<T>> listeners = new HashSet<>();

    /**
     * Determines whether the model currently holds valid data.
     * Even if the model is not valid, {@link #getData()} can still return
     * a default value that represents a safe fallback.
     *
     * @return {@code true} if the model's data is valid, {@code false} otherwise
     */
    public abstract boolean isValid();

    /**
     * Reads the current data from the underlying source.
     * Implementations should return any available data, even if not valid,
     * as long as it can be safely represented. If no meaningful value can be produced,
     * this method must return {@link Optional#empty()} instead of throwing an exception.
     *
     * @return an {@link Optional} containing the data if available, or empty if not readable
     */
    protected abstract Optional<T> readData();

    /**
     * Provides a default value for the model data.
     * The default is returned when no valid or readable value is available.
     * Implementations must ensure that this method never returns {@code null}.
     *
     * @return the default data value
     */
    protected abstract T getDefaultData();

    /**
     * Writes new data to the underlying source.
     * Implementations may modify internal validity state depending on whether
     * the data could be stored successfully. This method should not throw exceptions;
     * instead, return {@code false} to indicate a write failure or read-only behavior.
     *
     * @param data the new data to write
     * @return {@code true} if the write succeeded, or {@code false} if the model cannot be updated
     */
    protected abstract boolean writeData(T data);

    /**
     * Retrieves the current data.
     * If {@link #readData()} provides a value, that value is returned directly.
     * Otherwise, the model returns the default value from {@link #getDefaultData()}.
     * This method does <b>not</b> check validity; use {@link #isValid()} to determine
     * whether the returned data should be trusted.
     *
     * @return the current or default data (never {@code null})
     */
    public T getData() {
        Optional<T> data = this.readData();
        return data.orElseGet(this::getDefaultData);
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
     * Registers a new listener to receive model updates.
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
     * Notifies all registered listeners with the model's current data.
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
