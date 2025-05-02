/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

package com.kniazkov.widgets;

/**
 * Listener interface for receiving notifications when data changes.
 * Classes that are interested in processing data change events
 * should implement this interface and register themselves where necessary.
 * @param <T> The type of the data being observed
 */
public interface Listener<T> {
    /**
     * Invoked when the observed data has changed.
     * @param data The updated data
     */
    void dataChanged(T data);

    /**
     * Creates a combined listener that delegates events to all provided listeners.
     * <p>
     *     The resulting listener invokes {@link #dataChanged(Object)} on each target listener
     *     in the order they are given.
     * </p>
     *
     * @param listeners The listeners to combine (must not be null or contain nulls)
     * @param <T> The data type
     * @return A listener that forwards events to all provided listeners
     */
    @SafeVarargs
    static <T> Listener<T> combine(Listener<T>... listeners) {
        return data -> {
            for (Listener<T> listener : listeners) {
                listener.dataChanged(data);
            }
        };
    }
}
