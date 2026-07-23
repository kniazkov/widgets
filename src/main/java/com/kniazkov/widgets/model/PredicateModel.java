/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import java.util.function.Predicate;

/**
 * A read-only boolean model derived from another model using a predicate.
 *
 * @param <T> the type of data in the base model
 */
public final class PredicateModel<T> extends ReadOnlyModel<Boolean> {
    /**
     * The wrapped base model.
     */
    private final Model<T> base;

    /**
     * Predicate used to convert base model data to a boolean value.
     */
    private final Predicate<T> predicate;

    /**
     * Current cached boolean value.
     */
    private boolean value;

    /**
     * Listener attached to the base model.
     */
    private final Listener<T> forwarder;

    /**
     * Creates a new predicate-based boolean model.
     *
     * @param base the base model
     * @param predicate predicate that converts base data to boolean
     */
    public PredicateModel(final Model<T> base, final Predicate<T> predicate) {
        this.base = base;
        this.predicate = predicate;
        this.value = this.compute();

        this.forwarder = data -> {
            final boolean newValue = this.compute();
            if (this.value != newValue) {
                this.value = newValue;
                this.notifyListeners(newValue);
            }
        };

        this.base.addListener(this.forwarder);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Boolean getData() {
        return this.value;
    }

    /**
     * Returns a model that represents the logical negation of this model’s boolean value.
     *
     * @return a new model exposing {@code !getData()}
     */
    public Model<Boolean> invert() {
        return new InvertModel(this);
    }

    /**
     * Calculates a boolean value using a predicate
     * @return Calculated boolean value
     */
    private boolean compute() {
        return this.base.isValid() && this.predicate.test(this.base.getData());
    }
}