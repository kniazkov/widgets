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
     * Creates a new model instance that is <i>similar</i> to this one but initialized
     * with the specified data. The returned model should preserve the same behavioral
     * characteristics — such as validation logic, listener notification policy, and
     * synchronization strategy — but may represent an entirely new object or even a
     * different concrete implementation, depending on context.
     * <br>
     * This method is useful when you want to derive a model with new data but reuse the
     * semantics of the current model (for example, when transforming or cloning parts
     * of a reactive model hierarchy).
     *
     * @param data initial data value for the new model
     * @return a new model instance containing the given data and similar logic
     */
    Model<T> deriveWithData(final T data);

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

    /**
     * Creates a cascading view of this model.
     * <p>
     * The returned {@link CascadingModel} delegates all reads to this model until the first call
     * to {@link Model#setData(Object)}, at which point it forks into a local copy that can be
     * modified independently.
     *
     * @return a new cascading model wrapping this model
     */
    default Model<T> asCascading() {
        return new CascadingModel<>(this);
    }

    /**
     * Creates a thread-safe wrapper around this model.
     * <p>
     * The returned {@link SynchronizedModel} provides synchronized access to the underlying model
     * and re-emits all updates in a thread-safe manner.
     * <br>
     * Use this when the underlying model may be accessed from multiple threads, such as background
     * tasks or reactive pipelines.
     *
     * @return a new synchronized model wrapping this model
     */
    default Model<T> asSynchronized() {
        return new SynchronizedModel<>(this);
    }
}
