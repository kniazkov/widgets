/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Data, inaccessible now, but which will probably be available it the future.
 * @param <T> Type of data
 */
public final class Future<T> {
    /**
     * Set of listeners.
     */
    private final Set<Listener<T>> listeners;

    /**
     * Data.
     */
    private T data;

    /**
     * Constructor.
     */
    public Future() {
        this.listeners = new HashSet<>();
        this.data = null;
    }

    /**
     * Adds a listener. Listeners are notified as soon as the data is set.
     * If the data is already present, the listener is called immediately.
     * @param listener Listener
     */
    public void addListener(@NotNull Listener<T> listener) {
        if (this.data != null) {
            listener.dataChanged(this.data);
        } else {
            this.listeners.add(listener);
        }
    }

    /**
     * Removes the listener.
     * @param listener Listener
     */
    public void removeListener(@NotNull Listener<T> listener) {
        this.listeners.remove(listener);
    }

    /**
     * Sets data. This can be done only once.
     * @param data Data
     */
    public void setData(@NotNull T data) {
        assert this.data == null;
        this.data  = data;
        for (final Listener<T> listener : this.listeners) {
            listener.dataChanged(data);
        }
        this.listeners.clear();
    }
}
