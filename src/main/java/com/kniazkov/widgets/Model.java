/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Abstract base class representing the "Model" component in the Model-View-Controller (MVC)
 * architectural pattern.
 *
 * <p>
 *     In the MVC pattern, the Model is responsible for holding application data and business logic.
 *     It abstracts the underlying data source and provides a consistent interface for querying and
 *     updating data, while notifying interested parties (typically Views or Controllers) about
 *     changes.
 * </p>
 *
 * <p>
 *     This specific implementation introduces <b>prototype-based inheritance</b> for models.
 *     Instead of relying solely on direct state, a model instance can inherit behavior and data
 *     from another instance—its prototype. The prototype serves as a fallback source of truth
 *     when the current model lacks valid data. If a model has no data of its own and is not in a
 *     valid state, but a prototype exists, data will be transparently delegated to the prototype.
 * </p>
 *
 * <p>
 *     The relationship is unidirectional: the child model listens to the prototype for data
 *     changes. Once the child sets its own value (thereby gaining valid state), it detaches from
 *     the prototype and becomes independent. This pattern allows efficient model reuse and lazy
 *     divergence of state.
 * </p>
 *
 * <p>
 *     Expected behavior from implementations:
 * </p>
 * <ul>
 *     <li>
 *         Immediately after creation, the model is considered <b>invalid</b>: it has no meaningful
 *         data of its own.
 *     </li>
 *     <li>
 *         The {@link #getData()} method always returns a non-null, meaningful value regardless of
 *         internal validity, by either falling back to the prototype or returning a default value
 *         provided by {@link #getDefaultData()}.
 *     </li>
 *     <li>
 *         When a value is set using {@link #setData(Object)}, it is first validated by the model
 *         implementation.
 *         If the value is considered invalid, it is not accepted nor propagated to listeners.
 *     </li>
 * </ul>
 *
 * @param <T> the type of data managed by this model
 */
public abstract class Model<T> {
    /**
     * Optional reference to a prototype model from which this model instance may inherit data.
     * <p>
     *     If this field is non-null, the current model is considered a fork of another model.
     *     In this case, {@link #getData()} will delegate to the prototype if the current model
     *     lacks valid data. When the current model sets its own data, it detaches from the
     *     prototype and becomes self-contained.
     * </p>
     */
    private Prototype<T> prototype;

    /**
     * List of weak references to listeners observing changes in this model's data.
     * <p>
     *     Listeners are notified whenever the model's data is modified.
     *     Weak references are used to avoid memory leaks, allowing listeners to be garbage
     *     collected if they are no longer strongly reachable elsewhere in the application.
     * </p>
     */
    private final List<WeakReference<Listener<T>>> listeners;

    /**
     * Constructor.
     */
    protected Model() {
        this.listeners = new LinkedList<>();
    }

    /**
     * Creates a new instance of the same model type.
     * <p>
     *     This method is used when forking the current model via {@link #fork()}.
     *     The returned instance must be of the same concrete class as the original and must
     *     be in an initial, invalid state.
     * </p>
     *
     * @return A new, uninitialized instance of the same model
     */
    protected abstract Model<T> createInstance();

    /**
     * Checks whether the model currently holds valid data.
     * <p>
     *     This method determines whether {@link #readData()} is expected to return meaningful data.
     *     Models are typically invalid immediately after creation.
     * </p>
     *
     * @return {@code true} if the model holds valid data, {@code false} otherwise
     */
    public abstract boolean isValid();

    /**
     * Returns the current data value stored in the model, if any.
     * <p>
     *     If the model is invalid or uninitialized, this method should return
     *     {@link Optional#empty()}.
     *     Otherwise, it must return a non-null value wrapped in {@link Optional}.
     * </p>
     *
     * @return an {@link Optional} containing the current value, or empty if none
     */
    protected abstract Optional<T> readData();

    /**
     * Provides a fallback default value for the model.
     * <p>
     *     This method must always return a non-null, valid default value of type {@code T}.
     *     It is used when the model does not hold any data and has no valid prototype.
     * </p>
     *
     * @return A non-null default value of type {@code T}
     */
    protected abstract T getDefaultData();

    /**
     * Attempts to write the given value into the model.
     * <p>
     *     Before writing, the value must be validated by the model.
     *     If the value is considered invalid, the method must not update internal state
     *     and must return {@code false}.
     *     If the value is valid and written successfully, the model becomes valid
     *     (if it wasn't already) and the method returns {@code true}.
     * </p>
     *
     * @param data The new value to store; must be non-null
     * @return {@code true} if the value was accepted and written; {@code false} otherwise
     */
    protected abstract boolean writeData(T data);

    /**
     * Creates a forked instance of this model, establishing a prototype-based relationship.
     * <p>
     *     The new model instance is created using {@link #createInstance()} and begins in an
     *     invalid state, without its own data. It references this model as its prototype.
     * </p>
     *
     * <p>
     *     While the forked model remains invalid, calls to {@link #getData()} will delegate to
     *     the prototype, effectively inheriting its data. Additionally, the fork registers itself
     *     as a listener to the prototype and will be notified of any changes to the prototype's
     *     data. This ensures that unmodified forks remain synchronized with the prototype.
     * </p>
     *
     * <p>
     *     Once the forked model sets its own data (via {@link #setData(Object)}), it detaches
     *     from the prototype: it stops listening to changes in the prototype and starts acting
     *     as a standalone model instance.
     * </p>
     *
     * @return a new model instance that inherits data and notifications from this model until modified
     */
    public Model<T> fork() {
        final Model<T> fork = this.createInstance();
        final Listener<T> listener = fork::notifyListeners;
        fork.prototype = new Prototype<>();
        fork.prototype.model = this;
        fork.prototype.listener = listener;
        this.addListener(listener);
        return fork;
    }

    /**
     * Retrieves the current data value from the model.
     * <p>
     *     This method guarantees a non-null, meaningful result under all conditions.
     *     The resolution logic proceeds as follows:
     * </p>
     * <ol>
     *     <li>
     *         If the model is {@link #isValid() valid}, it attempts to retrieve its own data
     *         via {@link #readData()}.
     *     </li>
     *     <li>If the data is present, it is returned.</li>
     *     <li>
     *         If the model is invalid but has a prototype, the call is delegated to the
     *         prototype’s {@code getData()} method.
     *     </li>
     *     <li>
     *         If no valid data is found in either this model or the prototype chain,
     *         the {@link #getDefaultData()} value is returned.
     *     </li>
     * </ol>
     *
     * <p>
     *     This design ensures that consumers of the model never receive {@code null} and never
     *     need to check for data presence manually.
     * </p>
     *
     * @return a non-null, valid value of type {@code T}
     */
    public T getData() {
        if (this.isValid()) {
            Optional<T> data = this.readData();
            if (data.isPresent()) {
                return data.get();
            }
        }
        if (this.prototype != null) {
            return this.prototype.model.getData();
        }
        return this.getDefaultData();
    }

    /**
     * Attempts to update the model's data with the given value.
     * <p>
     *     The behavior depends on the current state of the model:
     * </p>
     * <ul>
     *     <li>
     *         If the model already contains data (i.e., {@link #readData()} returns present),
     *         the new value is compared to the old one.
     *         If they are equal, no action is taken. If different, the new value is validated
     *         and written via {@link #writeData(Object)}.
     *         On success, listeners are notified.
     *     </li>
     *     <li>
     *         Else, if the model is currently forked from a prototype, the new value is
     *         validated and written.
     *         If the new value differs from the inherited one (from {@code prototype.getData()}),
     *         listeners are notified.
     *         Then the model detaches from the prototype and becomes independent.
     *     </li>
     *     <li>
     *         Else, if the model is invalid and has no prototype, the new value is compared
     *         to the {@link #getDefaultData()}.
     *         If different and valid, it is written and listeners are notified.
     *     </li>
     * </ul>
     *
     * <p>
     *     All write operations are gated by {@link #writeData(Object)}.
     *     If the new value is not valid, it is not stored and no listeners are notified.
     * </p>
     *
     * @param newData the value to assign to this model; must not be {@code null}
     */
    public void setData(final T newData) {
        final Optional<T> oldData = this.readData();
        if (oldData.isPresent()) {
            if (!oldData.get().equals(newData)) {
                if (writeData(newData)) {
                    this.notifyListeners(newData);
                }
            }
        } else if (this.prototype != null) {
            if (writeData(newData)) {
                if (!this.prototype.model.getData().equals(newData)) {
                    this.notifyListeners(newData);
                }
            }
            this.prototype.model.removeListener(this.prototype.listener);
            this.prototype = null;
        } else {
            if (!getDefaultData().equals(newData)) {
                if (writeData(newData)) {
                    this.notifyListeners(newData);
                }
            }
        }
    }

    /**
     * Registers a new listener to observe changes in the model's data.
     * <p>
     *     The listener will be notified via {@link Listener#dataChanged(Object)} whenever
     *     the model's data is updated through {@link #setData(Object)}, assuming the change
     *     is accepted and results in a new value.
     * </p>
     *
     * <p>
     *     Listeners are stored using {@link WeakReference}, meaning they do not prevent
     *     garbage collection. This avoids memory leaks when listeners are no longer in active
     *     use elsewhere.
     * </p>
     *
     * @param listener the listener to register; must not be {@code null}
     */
    public void addListener(final Listener<T> listener) {
        this.listeners.add(new WeakReference<>(listener));
    }

    /**
     * Unregisters a previously added listener.
     * <p>
     *     If the listener is currently registered, it will be removed from the list of observers.
     *     If the listener is not found, or has already been garbage collected
     *     (due to weak referencing), this method does nothing.
     * </p>
     *
     * <p>
     *     This method also cleans up any stale references to already collected listeners
     *     during the iteration.
     * </p>
     *
     * @param listener the listener to remove; must not be {@code null}
     */    public void removeListener(final Listener<T> listener) {
        final Iterator<WeakReference<Listener<T>>> iterator = this.listeners.iterator();
        while (iterator.hasNext()) {
            final WeakReference<Listener<T>> reference = iterator.next();
            final Listener<T> existing = reference.get();
            if (existing == listener) {
                iterator.remove();
                break;
            } else if (existing == null) {
                iterator.remove();
            }
        }
    }

    /**
     * Notifies all currently registered listeners with the model's current data.
     * <p>
     *     The data passed to listeners is obtained via {@link #getData()}, ensuring that
     *     the value is always non-null and valid, regardless of internal state.
     * </p>
     *
     * <p>
     *     During notification, any stale (collected) weak references are cleaned up.
     * </p>
     */
    public void notifyListeners() {
        this.notifyListeners(this.getData());
    }

    /**
     * Notifies all currently registered listeners with the provided data value.
     * <p>
     *     This method is typically called after a successful update to the model's data.
     *     It directly invokes {@link Listener#dataChanged(Object)} on each active listener.
     * </p>
     *
     * <p>
     *     Listeners are held via {@link WeakReference}, and any that have been garbage collected
     *     are removed during the notification process.
     * </p>
     *
     * @param data the data to pass to listeners; must be non-null and considered valid
     */
    protected void notifyListeners(final T data) {
        final Iterator<WeakReference<Listener<T>>> iterator = this.listeners.iterator();
        while (iterator.hasNext()) {
            final WeakReference<Listener<T>> reference = iterator.next();
            final Listener<T> listener = reference.get();
            if (listener == null) {
                iterator.remove();
            } else {
                listener.dataChanged(data);
            }
        }
    }

    /**
     * Detaches this model from its prototype, if currently linked.
     * <p>
     *     This method is intended to be used by subclasses that define custom data mutation
     *     methods outside of the standard {@link #setData(Object)} flow. When such a method
     *     modifies the model's data, it must call {@code detach()} to ensure the model
     *     no longer inherits from or listens to its prototype.
     * </p>
     *
     * <p>
     *     This method removes the listener connection to the prototype and clears the reference.
     *     It has no effect if the model is not currently forked.
     * </p>
     *
     * <p>
     *     This method is {@code protected} to prevent external consumers from forcibly breaking
     *     the prototype chain — the only supported public way to do that is via
     *     {@link #setData(Object)}.
     * </p>
     */
    protected void detach() {
        if (this.prototype != null) {
            this.prototype.model.removeListener(this.prototype.listener);
            this.prototype = null;
        }
    }

    /**
     * Internal helper class representing a prototype link in a forked model.
     * <p>
     *     Each instance of {@code Model} that is forked from another contains a {@code Prototype}
     *     holding both a reference to the parent model and a listener that keeps the child in sync
     *     with the parent's data changes.
     * </p>
     *
     * <p>
     *     This structure is used internally and is not exposed to external consumers.
     *     It enables the prototype-based inheritance and listener chain between parent
     *     and forked models.
     * </p>
     *
     * @param <T> the type of data held by the model
     */
    private static class Prototype<T> {

        /**
         * The parent model from which data is inherited until overridden.
         */
        Model<T> model;

        /**
         * The listener instance that is registered on the prototype model to receive data change
         * notifications.
         */
        Listener<T> listener;
    }
}
