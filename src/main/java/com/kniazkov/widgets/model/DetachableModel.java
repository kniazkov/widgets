/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.view.RootWidget;
import java.util.Optional;

/**
 * A model wrapper that automatically detaches its listener when the root widget is destroyed.
 * This class is designed for use with global or shared models that outlive individual clients.
 * When attached to a specific {@link RootWidget}, the wrapper listens for changes in the base model
 * and forwards them to its own listeners. When the root is closed, it automatically unsubscribes
 * from the base model, preventing memory leaks and dangling references.
 *
 * @param <T> the type of the data managed by this model
 */
public class DetachableModel<T> extends Model<T> {

    /**
     * The underlying base model being wrapped.
     */
    private final Model<T> base;

    /**
     * The listener registered on the base model.
     */
    private final Listener<T> listener;

    /**
     * Creates a detachable wrapper around the given base model.
     * The wrapper subscribes to updates from the base model and automatically
     * detaches when the specified {@link RootWidget} is destroyed.
     *
     * @param root the root widget that defines the lifetime of this model
     * @param base the base model to wrap
     */
    public DetachableModel(final RootWidget root, final Model<T> base) {
        this.base = base;
        this.listener = this::notifyListeners;
        base.addListener(this.listener);
        root.onClose(() -> base.removeListener(this.listener));
    }

    /**
     * Manually detaches this model from the base model.
     * This method is safe to call multiple times.
     */
    public void detach() {
        this.base.removeListener(this.listener);
    }

    @Override
    public boolean isValid() {
        return this.base.isValid();
    }

    @Override
    protected Optional<T> readData() {
        return this.base.readData();
    }

    @Override
    protected T getDefaultData() {
        return this.base.getDefaultData();
    }

    @Override
    protected boolean writeData(final T data) {
        return this.base.writeData(data);
    }
}
