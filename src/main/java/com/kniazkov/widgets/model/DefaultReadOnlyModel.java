/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * A simple immutable {@link ReadOnlyModel} that always provides a constant value.
 * The {@code DefaultReadOnlyModel} serves as a lightweight wrapper around static, unchanging data.
 * It is always considered valid and never emits any updates, making it ideal for use with fixed
 * properties such as default colors, fonts, or layout parameters that do not depend on
 * application state.

 * @param <T> the type of the constant data managed by this model
 */
public final class DefaultReadOnlyModel<T> extends ReadOnlyModel<T> {
    /**
     * The constant data value represented by this model.
     */
    private final T data;

    /**
     * Creates a new immutable model holding the specified constant value.
     *
     * @param data the static value to store in this model
     */
    public DefaultReadOnlyModel(final T data) {
        this.data = data;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public T getData() {
        return this.data;
    }

    @Override
    public T getDefaultData() {
        return this.data;
    }
}
