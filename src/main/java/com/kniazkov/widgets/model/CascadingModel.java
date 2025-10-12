/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import java.util.Optional;

/**
 * A cascading model that delegates to a base model until overridden.
 * <p>
 * This wrapper implements a "lazy fork" mechanism:
 * <ul>
 *   <li>As long as no local model exists, all read operations are delegated to the base model.</li>
 *   <li>When {@link #setData(Object)} is first called, a new local model is created using the
 *       provided {@link ModelFactory}, and all subsequent reads and writes go through it.</li>
 *   <li>The base model is never modified by this wrapper — updates remain local.</li>
 * </ul>
 * <p>
 * This design enables hierarchical or cascading data propagation.
 * For example, a base style can define shared properties (e.g. background color),
 * while individual instances can locally override them without breaking linkage
 * to the base model for others.
 *
 * @param <T> the type of data managed by this model
 */
public final class CascadingModel<T> extends Model<T> {
    /**
     * The base model from which this cascading model derives its data.
     * */
    private final Model<T> base;

    /**
     * Factory for creating local models when an override is needed.
     */
    private final ModelFactory<T> factory;

    /**
     * The lazily created local model, or {@code null} if not overridden.
     */
    private Model<T> local;

    private final Listener<T> listener;

    /**
     * Creates a new cascading model.
     *
     * @param base the base model to delegate to until overridden
     * @param factory factory that creates new local models
     */
    public CascadingModel(final Model<T> base, final ModelFactory<T> factory) {
        this.base = base;
        this.factory = factory;
        this.listener = this::notifyListeners;

        base.addListener(this.listener);
    }

    @Override
    public boolean isValid() {
        return (this.local != null) ? this.local.isValid() : this.base.isValid();
    }

    @Override
    protected Optional<T> readData() {
        return (this.local != null) ? this.local.readData() : this.base.readData();
    }

    @Override
    protected T getDefaultData() {
        return (this.local != null) ? this.local.getDefaultData() : this.base.getDefaultData();
    }

    @Override
    protected boolean writeData(final T data) {
        if (this.local == null) {
            this.base.removeListener(this.listener);
            this.local = this.factory.create(data);
            this.local.addListener(this.listener);
            this.notifyListeners(data);
            return true;
        } else {
            return this.local.setData(data);
        }
    }
}
