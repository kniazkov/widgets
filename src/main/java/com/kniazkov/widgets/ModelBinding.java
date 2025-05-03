/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Binds a non-null {@link Model} to a fixed, non-null {@link Listener}.
 * <p>
 *     This class manages the listener's attachment: when the model is changed,
 *     the listener is automatically removed from the old model and added to the new one.
 *     Both the model and the listener are guaranteed to be non-null.
 * </p>
 *
 * @param <T> The type of data in the model
 */
public final class ModelBinding<T> {
    private Model<T> model;
    private final Listener<T> listener;

    /**
     * Creates a binding between the given model and listener.
     *
     * @param model The initial model (must not be null)
     * @param listener The fixed listener (must not be null)
     */
    public ModelBinding(Model<T> model, Listener<T> listener) {
        this.listener = listener;
        this.model = model;
        this.model.addListener(this.listener);
        this.model.notifyListeners();
    }

    /**
     * Returns the current model.
     *
     * @return The bound model
     */
    public Model<T> getModel() {
        return this.model;
    }

    /**
     * Sets a new model, re-binding the fixed listener.
     * Removes the listener from the old model and adds it to the new one.
     *
     * @param model The new model (must not be null)
     */
    public void setModel(Model<T> model) {
        if (this.model != model) {
            this.model.removeListener(this.listener);
            this.model = model;
            this.model.addListener(this.listener);
        }
    }
}
