/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Standard model, which is used by default and is suitable for most cases.
 * It does not contain any logic to validate the data, any data is considered valid.
 * It simply stores data of the specified type and notifies listeners when that data changes.
 * @param <T> Type of model data
 */
public abstract class DefaultModel<T> extends Model<T> {
    /**
     * Data.
     */
    private T data;

    /**
     * Constructor.
     */
    public DefaultModel() {
        this.data = this.getDefaultData();
    }

    @Override
    public @NotNull T getData() {
        return this.data;
    }

    @Override
    protected boolean writeData(final @NotNull T data) {
        Objects.requireNonNull(data);
        this.data = data;
        return true;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    /**
     * Returns the default data that the model contains after creation and before any changes.
     * @return Default data.
     */
    public abstract @NotNull T getDefaultData();
}
