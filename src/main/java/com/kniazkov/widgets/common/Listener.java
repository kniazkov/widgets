/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import java.util.Arrays;

/**
 * A generic listener interface for receiving notifications about data changes.
 *
 * @param <T> the type of the data passed to the listener
 */
public interface Listener<T> {

    /**
     * Called when the observed data has changed.
     *
     * @param data the updated data
     */
    void accept(T data);

    /**
     * Combines multiple listeners into a single one.
     * The returned listener will notify each provided listener in order.
     *
     * @param listeners the listeners to combine
     * @param <T> the type of data
     * @return a combined listener that delegates calls to all provided listeners
     */
    @SafeVarargs
    static <T> Listener<T> combine(final Listener<T>... listeners) {
        final Listener<T>[] copy = Arrays.copyOf(listeners, listeners.length);
        return data -> {
            for (Listener<T> listener : copy) {
                listener.accept(data);
            }
        };
    }
}
