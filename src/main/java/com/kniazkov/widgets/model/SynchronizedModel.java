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
public final class SynchronizedModel<T> implements Model<T> {

    /**
     * The wrapped base model that provides the actual data and validation logic.
     * All direct access to this model is synchronized on {@link #lock}.
     */
    private final Model<T> base;

    /**
     * The synchronization lock used to serialize access to the underlying model’s data
     * and listeners.
     */
    private final Object lock;

    /**
     * A set of listeners registered on this wrapper.
     */
    private final Set<Listener<T>> listeners;

    /**
     * Creates a new synchronized wrapper for the specified base model.
     *
     * @param base the model to wrap (must not be {@code null})
     */
    public SynchronizedModel(final Model<T> base) {
        this.base = base;
        this.lock = new Object();
        this.listeners = new HashSet<>();

        base.addListener(this::notifyListeners);
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
    public int addListener(final Listener<T> listener) {
        final int size;
        synchronized (this.lock) {
            this.listeners.add(listener);
            size = this.listeners.size();
        }
        return size;
    }

    @Override
    public int removeListener(final Listener<T> listener) {
        final int size;
        synchronized (this.lock) {
            this.listeners.remove(listener);
            size = this.listeners.size();
        }
        return size;
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
    private void notifyListeners(final T data) {
        for (Listener<T> listener : this.listeners) {
            listener.accept(data);
        }
    }
}
