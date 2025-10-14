/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;

/**
 * A utility class for binding a {@link Model} to a {@link Listener}.
 * This binding ensures that the listener is always attached to the current model,
 * and makes it easy to switch to another model without manually managing
 * listener registration.
 *
 * @param <T> the type of the model data
 */
public final class ModelBinding<T> {
    /**
     * The currently bound model. May be {@code null} when detached.
     */
    private Model<T> model;

    /**
     * The listener associated with this binding.
     * This reference is constant during the binding’s lifetime.
     */
    private final Listener<T> listener;

    /**
     * The factory used to create new models when no model is currently set.
     */
    private final ModelFactory<T> factory;

    /**
     * Creates a new binding between a model and a listener, using the specified factory
     * for lazy model creation.
     *
     * @param model the initial model to bind
     * @param listener the listener that will receive model updates
     * @param factory  the factory used to lazily create models when needed
     */
    public ModelBinding(final Model<T> model,
                        final Listener<T> listener,
                        final ModelFactory<T> factory) {
        this.model = model;
        this.listener = listener;
        this.factory = factory;

        listener.accept(model.getData());
        model.addListener(listener);
    }

    /**
     * Returns the currently bound model.
     * If the current model is {@code null}, a new one is created via the factory,
     * attached to the listener, stored, and returned.
     *
     * @return a non-null model instance bound to this listener
     */
    public Model<T> getModel() {
        if (this.model == null) {
            final Model<T> placeholder = this.factory.create();
            placeholder.addListener(this.listener);
            this.model = placeholder;
            this.listener.accept(placeholder.getData());
        }
        return this.model;
    }

    /**
     * Rebinds this binding to a different model.
     *
     * @param model the new model to bind to (may be {@code null})
     */
    public void setModel(final Model<T> model) {
        if (this.model == model) {
            return;
        }
        if (this.model != null) {
            this.model.removeListener(this.listener);
        }
        this.model = model;
        if (model != null) {
            model.addListener(this.listener);
            this.listener.accept(model.getData());
        }
    }
}
