/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * A read-only version of {@link SingleThreadModel}.
 * This model exposes the same data retrieval API but ignores any attempts to modify its value.
 * It is typically used when a model represents static or externally managed data.
 *
 * @param <T> the type of data managed by this model
 */
public abstract class ReadOnlyModel<T> extends SingleThreadModel<T> {
    @Override
    public boolean setData(T data) {
        return false;
    }

    /**
     * Creates a new {@link ReadOnlyModel} instance initialized with the specified data.
     *
     * @param data the static value for the new model
     * @return a new read-only model instance containing {@code data}
     */
    public static <T> ReadOnlyModel<T> create(T data) {
        return new DefaultReadOnlyModel<>(data);
    }
}
