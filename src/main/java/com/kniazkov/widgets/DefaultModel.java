/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.util.Objects;
import java.util.Optional;

/**
 * Standard model, which is used by default and is suitable for most cases.
 * It does not contain any logic to validate the data, any data is considered valid.
 * It simply stores data of the specified type and notifies listeners when that data changes.
 * @param <T> Type of model data
 */
public abstract class DefaultModel<T> extends Model<T> {
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
