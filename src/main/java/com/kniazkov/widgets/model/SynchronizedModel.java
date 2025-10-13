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
     * <p>
     * All direct access to this model is synchronized on {@link #dataLock}.
     * </p>
     */
    private final Model<T> base;

    /**
     * The synchronization lock used to serialize access to the underlying model’s data.
     */
    private final Object dataLock = new Object();

    /**
     * The synchronization lock used to protect listener registration and removal.
     * Separating this lock from {@link #dataLock} reduces contention when listeners
     * are frequently added or removed while other threads update data.
     */
    private final Object listenerLock = new Object();

    /**
     * A thread-safe set of listeners registered on this wrapper.
     * <p>
     * Iteration and modification are protected by {@link #listenerLock}.
     * </p>
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
        synchronized (this.dataLock) {
            return this.base.isValid();
        }
    }

    @Override
    public T getData() {
        synchronized (this.dataLock) {
            return this.base.getData();
        }
    }

    @Override
    public T getDefaultData() {
        return this.base.getDefaultData();
    }

    @Override
    public boolean setData(final T data) {
        synchronized (this.dataLock) {
            return this.base.setData(data);
        }
    }

    @Override
    public void addListener(final Listener<T> listener) {
        synchronized (this.listenerLock) {
            this.listeners.add(listener);
        }
    }

    @Override
    public void removeListener(final Listener<T> listener) {
        synchronized (this.listenerLock) {
            this.listeners.remove(listener);
        }
    }

    @Override
    public void notifyListeners() {
        this.notifyListeners(this.getData());
    }

    /**
     * Notifies all registered listeners with the specified data value.
     * This method copies the listener set under {@link #listenerLock} and then delivers
     * notifications outside of any synchronization to avoid
     * reentrancy and deadlocks.
     *
     * @param data the data value to broadcast to listeners
     */
    protected void notifyListeners(final T data) {
        final List<Listener<T>> copy;
        synchronized (this.listenerLock) {
            copy = new ArrayList<>(this.listeners);
        }
        for (Listener<T> listener : copy) {
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
