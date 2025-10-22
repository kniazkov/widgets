/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * A read-only variant of {@link SingleThreadModel} that represents immutable or externally
 * managed data within the reactive model hierarchy.
 * <p>
 * This abstract base class provides the same data access interface as a mutable model,
 * but ignores any modification attempts. Calling {@link #setData(Object)} on a read-only
 * model always returns {@code false} and never changes the underlying data.
 *
 * @param <T> the type of data managed by this model
 */
public abstract class ReadOnlyModel<T> extends SingleThreadModel<T> {
    @Override
    public boolean setData(final T data) {
        return false;
    }

    @Override
    public Model<T> deriveWithData(final T data) {
        return ReadOnlyModel.create(data);
    }

    /**
     * Creates a simple immutable {@link Model} instance that always returns the given data.
     *
     * @param <T>  the type of data stored in the model
     * @param data the value to expose
     * @return a new read-only model holding the specified data
     */
    public static <T> Model<T> create(final T data) {
        return new ReadOnlyModel<T>() {
            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public T getData() {
                return data;
            }
        };
    }
}
