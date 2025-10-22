/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe reactive wrapper for another {@link Model} instance.
 * <p>
 * This wrapper synchronizes access to an underlying (potentially non-thread-safe)
 * model using a {@link ReentrantLock} and maintains its own independent listener registry
 * backed by a {@link WeakHashMap}. Listeners are automatically removed when they
 * are garbage-collected, preventing memory leaks.
 *
 * @param <T> the type of the data managed by this model
 */
public final class SynchronizedModel<T> implements Model<T> {
    /**
     * The wrapped base model that provides the actual data and validation logic.
     * All access is guarded by {@link #lock}.
     */
    private final Model<T> base;

    /**
     * A reentrant synchronization lock used to serialize and coordinate access
     * to the underlying model’s state and listener registry.
     */
    private final ReentrantLock lock;

    /**
     * A registry of listeners that observe changes from this wrapper.
     */
    private final Map<Listener<T>, Object> listeners;

    /**
     * A listener that forwards updates from the base model to this wrapper’s listeners.
     * It must be stored in a field, not in a local variable, so that the garbage collector
     * does not remove the listener from the base model while this wrapper exists.
     */
    private final Listener<T> forwarder;

    /**
     * Creates a new synchronized wrapper for the specified base model.
     *
     * @param base the model to wrap
     */
    public SynchronizedModel(final Model<T> base) {
        this.base = base;
        this.lock = new ReentrantLock();
        this.listeners = new WeakHashMap<>();
        this.forwarder = this::notifyListeners;
        this.base.addListener(this.forwarder);
    }

    @Override
    public boolean isValid() {
        this.lock.lock();
        try {
            return base.isValid();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public T getData() {
        this.lock.lock();
        try {
            return base.getData();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public boolean setData(final T data) {
        this.lock.lock();
        try {
            return base.setData(data);
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void addListener(final Listener<T> listener) {
        this.lock.lock();
        try {
            listeners.put(listener, Boolean.TRUE);
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void removeListener(final Listener<T> listener) {
        this.lock.lock();
        try {
            listeners.remove(listener);
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void notifyListeners() {
        final T data;
        final List<Listener<T>> snapshot;
        this.lock.lock();
        try {
            data = base.getData();
            snapshot = new ArrayList<>(this.listeners.keySet());
        } finally {
            this.lock.unlock();
        }
        for (final Listener<T> listener : snapshot) {
            listener.accept(data);
        }
    }

    @Override
    public Model<T> deriveWithData(final T data) {
        return base.deriveWithData(data);
    }

    @Override
    public Model<T> asSynchronized() {
        // Returns this instance itself, since it is already a thread-safe model
        return this;
    }

    /**
     * Notifies all currently alive listeners with the specified data.
     *
     * @param data the data value to broadcast to listeners
     */
    protected void notifyListeners(final T data) {
        final List<Listener<T>> snapshot;
        this.lock.lock();
        try {
            snapshot = new ArrayList<>(this.listeners.keySet());
        } finally {
            this.lock.unlock();
        }
        for (final Listener<T> listener : snapshot) {
            listener.accept(data);
        }
    }
}
