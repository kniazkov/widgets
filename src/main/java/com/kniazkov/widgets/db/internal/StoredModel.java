/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.internal;

import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.db.Field;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.SynchronizedModel;

/**
 * A field model whose writes are committed through its owning store.
 *
 * @param <T> value type
 */
final class StoredModel<T> implements Model<T> {
    /**
     * Owning record.
     */
    private final MemoryRecord record;

    /**
     * Field.
     */
    private final Field<T> field;

    /**
     * Thread-safe observable value.
     */
    private final SynchronizedModel<T> delegate;

    /**
     * Creates a stored model.
     *
     * @param record record
     * @param field field
     * @param value initial value
     */
    StoredModel(
        final MemoryRecord record,
        final Field<T> field,
        final T value
    ) {
        this.record = record;
        this.field = field;
        this.delegate = field.getType().createModel(value).asSynchronized();
    }

    /**
     * Returns the field.
     *
     * @return field
     */
    Field<T> getField() {
        return this.field;
    }

    /**
     * Applies an already persisted value.
     *
     * @param value value
     */
    void apply(final T value) {
        this.delegate.setData(value);
    }

    @Override
    public boolean isValid() {
        return this.delegate.isValid();
    }

    @Override
    public T getData() {
        return this.delegate.getData();
    }

    @Override
    public boolean setData(final T data) {
        return this.record.update(this, data);
    }

    @Override
    public void addListener(final Listener<T> listener) {
        this.delegate.addListener(listener);
    }

    @Override
    public void removeListener(final Listener<T> listener) {
        this.delegate.removeListener(listener);
    }

    @Override
    public void notifyListeners() {
        this.delegate.notifyListeners();
    }

    @Override
    public Model<T> deriveWithData(final T data) {
        return this.field.getType().createModel(data);
    }
}
