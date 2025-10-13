/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import java.util.HashSet;
import java.util.Set;

/**
 * A single-threaded base implementation of the listener management logic
 * for reactive {@link Model} instances. This abstract class provides the infrastructure
 * for handling data change listeners within a model that is expected to be accessed from a single
 * thread (such as the UI thread). Subclasses are responsible for implementing the remaining
 * data-related behavior defined by the {@link Model} interface (e.g., data access, mutation,
 * and validation).
 *
 * @param <T> the type of the data managed by this model
 */public abstract class SingleThreadModel<T> implements Model<T> {
    /**
     * The collection of registered listeners that should be notified
     * whenever this model's data changes.
     */
    private final Set<Listener<T>> listeners = new HashSet<>();

    @Override
    public void addListener(final Listener<T> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(final Listener<T> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void notifyListeners() {
        this.notifyListeners(this.getData());
    }

    /**
     * Notifies all registered listeners with the specified data instance.
     * This method invokes {@link Listener#accept(Object)} on each registered listener
     * in the order returned by the underlying {@link HashSet} iterator.
     *
     * @param data the data object to be passed to each listener
     */
    protected void notifyListeners(final T data) {
        for (final Listener<T> listener : this.listeners) {
            listener.accept(data);
        }
    }
}
