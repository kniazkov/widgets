package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.*;

/**
 * A thread-safe reactive wrapper for another {@link Model} instance.
 * This wrapper provides synchronized access to an underlying (potentially non-thread-safe)
 * model and maintains its own independent, thread-safe set of listeners. It observes the
 * wrapped model and re-emits all updates to its own listeners, making it safe to use
 * from multiple threads concurrently.
 *
 * @param <T> the type of the data managed by this model
 */
public class SynchronizedModel<T> implements Model<T> {

    /**
     * The wrapped base model that provides the actual data and validation logic.
     * All direct access to this model is synchronized on {@link #lock}.
     */
    private final Model<T> base;

    /**
     * The synchronization lock used to serialize access to the underlying model’s data
     * and listeners.
     */
    private final Object lock = new Object();

    /**
     * A set of listeners registered on this wrapper.
     */
    private final Set<Listener<T>> listeners = new HashSet<>();

    /**
     * A listener that forwards updates from the base model to this wrapper’s listeners.
     */
    private final Listener<T> forwarder = this::notifyListeners;

    /**
     * Creates a new synchronized wrapper for the specified base model.
     * <p>
     * The wrapper automatically subscribes to updates from the base model.
     * </p>
     *
     * @param base the model to wrap (must not be {@code null})
     */
    public SynchronizedModel(final Model<T> base) {
        this.base = Objects.requireNonNull(base);
        this.base.addListener(this.forwarder);
    }

    @Override
    public boolean isValid() {
        synchronized (this.lock) {
            return this.base.isValid();
        }
    }

    @Override
    public T getData() {
        synchronized (this.lock) {
            return this.base.getData();
        }
    }

    @Override
    public T getDefaultData() {
        return this.base.getDefaultData();
    }

    @Override
    public boolean setData(final T data) {
        synchronized (this.lock) {
            return this.base.setData(data);
        }
    }

    @Override
    public void addListener(final Listener<T> listener) {
        synchronized (this.lock) {
            this.listeners.add(listener);
        }
    }

    @Override
    public void removeListener(final Listener<T> listener) {
        synchronized (this.lock) {
            this.listeners.remove(listener);
        }
    }

    @Override
    public void notifyListeners() {
        final List<Listener<T>> copy;
        final T data;
        synchronized (this.lock) {
            copy = new ArrayList<>(this.listeners);
            data = this.base.getData();
        }
        for (Listener<T> listener : copy) {
            listener.accept(data);
        }
    }

    /**
     * Notifies all registered listeners with the specified data value.
     *
     * @param data the data value to broadcast to listeners
     */
    protected void notifyListeners(final T data) {
        for (Listener<T> listener : this.listeners) {
            listener.accept(data);
        }
    }

    /**
     * Unsubscribes this wrapper from the base model.
     * After calling this method, this wrapper will no longer receive updates from the underlying
     * model. The registered listeners remain stored, but they will not be triggered automatically.
     */
    public void detach() {
        this.base.removeListener(this.forwarder);
    }
}
