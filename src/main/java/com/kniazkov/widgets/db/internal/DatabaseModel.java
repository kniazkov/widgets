/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.internal;

import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.model.Model;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.List;
import java.util.Set;

/**
 * Dispatcher-confined value with notifications deferred during batch installation.
 *
 * @param <T> value type
 */
final class DatabaseModel<T> implements Model<T> {
    /**
     * Dispatcher owner.
     */
    private final MemoryStore store;

    /**
     * Value and normalization behavior.
     */
    private Model<T> delegate;

    /**
     * Observers, accessed only on the dispatcher.
     */
    private final Set<Listener<T>> listeners = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * @param store dispatcher owner
     * @param delegate value
     */
    DatabaseModel(final MemoryStore store, final Model<T> delegate) {
        this.store = store;
        this.delegate = delegate;
    }

    /**
     * Installs an already validated model without invoking its normalization again.
     *
     * @param prepared validated value
     */
    void replace(final DatabaseModel<T> prepared) {
        final boolean changed = !java.util.Objects.equals(
            this.delegate.getData(), prepared.delegate.getData());
        this.delegate = prepared.delegate;
        if (changed) {
            this.notifyListeners();
        }
    }

    @Override
    public boolean isValid() {
        return this.store.call(this.delegate::isValid);
    }

    @Override
    public T getData() {
        return this.store.call(this.delegate::getData);
    }

    @Override
    public boolean setData(final T data) {
        return this.store.call(() -> {
            final boolean changed = this.delegate.setData(data);
            if (changed) {
                this.notifyListeners();
            }
            return changed;
        });
    }

    @Override
    public void addListener(final Listener<T> listener) {
        this.store.run(() -> this.listeners.add(java.util.Objects.requireNonNull(listener)));
    }

    @Override
    public void removeListener(final Listener<T> listener) {
        this.store.run(() -> this.listeners.remove(listener));
    }

    @Override
    public void notifyListeners() {
        this.store.run(() -> {
            for (final Listener<T> listener : List.copyOf(this.listeners)) {
                this.store.notifyAfterCommit(() -> listener.accept(this.delegate.getData()));
            }
        });
    }

    @Override
    public Model<T> deriveWithData(final T data) {
        return this.delegate.deriveWithData(data);
    }
}
