/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;

/**
 * Represents a reactive data model in an MVC architecture. A {@code Model} defines the contract
 * for managing a unit of application data — reading, writing, validating, and notifying listeners
 * about changes.
 * <br>
 * Models are used by controllers and views to observe data updates and reflect changes in the user
 * interface or application state. Implementations of this interface may define various storage
 * and synchronization strategies, but by default, models are <b>not thread-safe</b>
 * unless explicitly documented.
 *
 * @param <T> the type of the data managed by this model
 */
public interface Model<T> {
    /**
     * Indicates whether the model currently holds valid data.
     * Even if the model is not valid, {@link #getData()} must still return a usable fallback value.
     *
     * @return {@code true} if the model's data is valid, {@code false} otherwise
     */
    boolean isValid();

    /**
     * Returns the current data value of this model. Implementations should ensure this method
     * never returns {@code null}. The returned value should either represent the current data
     * (if available) or a reasonable default if the underlying source cannot provide data.
     *
     * @return the current or default data value (never {@code null})
     */
    T getData();

    /**
     * Provides a default value for the model data.
     * The default is returned when no valid or readable value is available.
     * Implementations must ensure that this method never returns {@code null}.
     *
     * @return the default data value
     */
    T getDefaultData();

    /**
     * Updates the model’s data if it differs from the current one, and notifies all registered
     * listeners if the update succeeds. If the model cannot be written to (e.g., is read-only
     * or persistence fails), this method must return {@code false}
     * instead of throwing an exception.
     *
     * @param data the new data to set
     * @return {@code true} if the data was changed and written successfully,
     *         {@code false} otherwise
     */
    boolean setData(final T data);

    /**
     * Registers a new listener that will be notified whenever the model's data changes.
     *
     * @param listener the listener to register
     */
    void addListener(final Listener<T> listener);

    /**
     * Unregisters a previously added listener.
     *
     * @param listener the listener to remove
     */
    void removeListener(final Listener<T> listener);

    /**
     * Notifies all registered listeners with the current data value.
     * Implementations should guarantee that all listeners receive the update
     * synchronously and in the order they were registered.
     */
    void notifyListeners();

    /**
     * Returns a reactive read-only {@link Model} representing the validity state of this model.
     * The returned model produces {@code true} when this model’s data is valid
     * (i.e. {@link #isValid()} returns {@code true}) and {@code false} otherwise.
     * It automatically updates whenever this model’s data or validity changes,
     * allowing UI components or controllers to observe validation state changes
     * in real time. This is especially useful in reactive UIs, where the validity flag can be
     * bound to visual indicators (such as red borders around invalid fields) or used to enable
     * and disable interactive controls.
     *
     * @return a read-only {@link Model} that reflects the validity state of this model
     */
    default Model<Boolean> getValidFlagModel() {
        return new ValidFlagModel<>(this);
    }
}
