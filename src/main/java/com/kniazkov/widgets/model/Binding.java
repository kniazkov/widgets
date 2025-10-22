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
public final class Binding<T> {
    /**
     * The currently bound model.
     */
    private Model<T> model;

    /**
     * The listener bound to the model.
     * This reference never changes during the lifetime of the binding.
     */
    private final Listener<T> listener;

    /**
     * Creates a new binding between the given model and listener.
     * Immediately invokes the listener with the model's current data,
     * then registers the listener for future updates.
     *
     * @param model the model to bind to
     * @param listener the listener that will receive model updates
     */
    public Binding(final Model<T> model, final Listener<T> listener) {
        this.listener = listener;
        this.listener.accept(model.getData());
        this.model = model;
        this.model.addListener(listener);
    }

    /**
     * Returns the currently bound model.
     *
     * @return the current model
     */
    public Model<T> getModel() {
        return this.model;
    }

    /**
     * Changes the bound model. Automatically removes the listener from the old model
     * and attaches it to the new model. If the new model is the same as the current one,
     * no action is taken.
     *
     * @param model the new model to bind to
     */
    public void setModel(Model<T> model) {
        if (this.model != model) {
            this.model.removeListener(this.listener);
            this.model = model;
            this.model.addListener(this.listener);
            this.listener.accept(model.getData());
        }
    }
}
