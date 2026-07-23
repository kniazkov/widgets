/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A single-threaded base implementation of the listener management logic
 * for reactive {@link Model} instances, using {@link WeakHashMap}-based listener storage.
 * <p>
 * This implementation prevents memory leaks caused by lingering strong references
 * to UI components or controllers that subscribe to a model but are no longer in use.
 *
 * @param <T> the type of data managed by this model
 */
public abstract class SingleThreadModel<T> implements Model<T> {
    /**
     * Weakly referenced listener registry.
     * When a listener is garbage-collected, its entry disappears automatically.
     */
    private final Map<Listener<T>, Object> listeners = new WeakHashMap<>();

    @Override
    public void addListener(final Listener<T> listener) {
        listeners.put(listener, Boolean.TRUE);
    }

    @Override
    public void removeListener(final Listener<T> listener) {
        listeners.remove(listener);
    }

    @Override
    public void notifyListeners() {
        notifyListeners(getData());
    }

    /**
     * Notifies all currently alive listeners with the specified data.
     * Dead (collected) listeners are automatically purged by the {@link WeakHashMap}.
     *
     * @param data the data object to pass to each listener
     */
    protected void notifyListeners(final T data) {
        for (Listener<T> listener : listeners.keySet()) {
            listener.accept(data);
        }
    }
}
