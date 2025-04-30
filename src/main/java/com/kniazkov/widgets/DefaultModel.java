/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.util.Objects;
import java.util.Optional;

/**
 * A standard, general-purpose implementation of {@link Model} that stores data directly and
 * applies no validation logic.
 *
 * <p>
 *     This model is suitable for the vast majority of use cases where the data is either
 *     inherently valid or validation is unnecessary. It accepts any non-null value as valid
 *     and provides simple storage and retrieval.
 * </p>
 *
 * @param <T> the type of data held by the model
 */
public abstract class DefaultModel<T> extends Model<T> {
    /**
     * The current value held by this model instance.
     * <p>
     *     This field serves as the internal storage for the model's data.
     *     It is considered valid if non-null, as determined by {@link #isValid()}.
     * </p>
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
