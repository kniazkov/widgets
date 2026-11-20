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
public final class Binding<T> implements AutoCloseable {
    /**
     * The currently bound model.
     */
    private Model<T> model;

    /**
     * Whether this listener has been permanently detached.
     */
    private boolean closed;

    /**
     * Framework-created view of a shared style model, never a model supplied by the caller.
     */
    private CascadingModel<T> styleModel;

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
     * Wraps a style model in a local owned cascade.
     *
     * @param base shared style source
     * @param listener view listener
     * @param <D> property type
     * @return binding owning only the local cascade
     */
    public static <D> Binding<D> ofStyle(final Model<D> base, final Listener<D> listener) {
        final CascadingModel<D> owned = new CascadingModel<>(base);
        try {
            final Binding<D> binding = new Binding<>(owned, listener);
            binding.styleModel = owned;
            return binding;
        } catch (final RuntimeException | Error failure) {
            owned.close();
            throw failure;
        }
    }

    /**
     * Replaces a borrowed or owned binding with a new local style cascade.
     *
     * @param base shared style source
     */
    public synchronized void setStyleModel(final Model<T> base) {
        if (this.closed) {
            throw new IllegalStateException("Binding is closed");
        }
        final CascadingModel<T> owned = new CascadingModel<>(base);
        try {
            this.setModel(owned);
        } finally {
            if (this.model == owned) {
                this.styleModel = owned;
            } else {
                owned.close();
            }
        }
    }

    /**
     * Detaches the upstream listener only for a privately owned cascade.
     */
    private void closeStyleModel() {
        if (this.styleModel != null) {
            this.styleModel.close();
            this.styleModel = null;
        }
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
    public synchronized void setModel(Model<T> model) {
        if (this.closed) {
            throw new IllegalStateException("Binding is closed");
        }
        if (this.model != model) {
            this.model.removeListener(this.listener);
            this.closeStyleModel();
            this.model = model;
            this.model.addListener(this.listener);
            this.listener.accept(model.getData());
        }
    }
    /**
     * Detaches only this listener, leaving the supplied model alive for its other users.
     */
    @Override
    public synchronized void close() {
        if (!this.closed) {
            this.closed = true;
            try {
                this.model.removeListener(this.listener);
            } finally {
                this.closeStyleModel();
            }
        }
    }
}
