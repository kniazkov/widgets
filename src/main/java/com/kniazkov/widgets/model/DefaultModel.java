/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Default in-memory implementation of {@link Model}.
 * This model simply stores the data in a private field and provides basic read/write functionality.
 * It is suitable for most use cases. The model considers itself valid if the internal data is not
 * {@code null}. The {@link #writeData(Object)} method guarantees that {@code null} values
 * are never stored.
 *
 * @param <T> the type of the data managed by this model
 */
public abstract class DefaultModel<T> extends Model<T> {
    /**
     * The internal data storage. May be {@code null} if no value has been set yet.
     */
    private T data;

    @Override
    public boolean isValid() {
        return data != null;
    }

    @Override
    protected Optional<T> readData() {
        return Optional.ofNullable(this.data);
    }

    @Override
    protected boolean writeData(final T data) {
        this.data = Objects.requireNonNull(data);
        return true;
    }
}
