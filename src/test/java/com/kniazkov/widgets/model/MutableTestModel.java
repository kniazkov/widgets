/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Controllable model used to exercise wrappers with both data and validity changes.
 *
 * @param <T> data type
 */
class MutableTestModel<T> implements Model<T> {
    /**
     * Current data.
     */
    private T data;

    /**
     * Current validity.
     */
    private boolean valid;

    /**
     * Registered listeners.
     */
    private final List<Listener<T>> listeners = new ArrayList<>();

    /**
     * Creates a valid model.
     *
     * @param data initial data
     */
    MutableTestModel(final T data) {
        this(data, true);
    }

    /**
     * Creates a model.
     *
     * @param data initial data
     * @param valid initial validity
     */
    MutableTestModel(final T data, final boolean valid) {
        this.data = data;
        this.valid = valid;
    }

    @Override
    public boolean isValid() {
        return this.valid;
    }

    @Override
    public T getData() {
        return this.data;
    }

    @Override
    public boolean setData(final T data) {
        if (Objects.equals(this.data, data)) {
            return false;
        }
        this.data = data;
        this.notifyListeners();
        return true;
    }

    /**
     * Changes validity and emits an update when it actually changes.
     *
     * @param valid new validity
     * @return whether validity changed
     */
    boolean setValid(final boolean valid) {
        if (this.valid == valid) {
            return false;
        }
        this.valid = valid;
        this.notifyListeners();
        return true;
    }

    @Override
    public void addListener(final Listener<T> listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    @Override
    public void removeListener(final Listener<T> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void notifyListeners() {
        for (final Listener<T> listener : new ArrayList<>(this.listeners)) {
            listener.accept(this.data);
        }
    }

    @Override
    public Model<T> deriveWithData(final T data) {
        return new MutableTestModel<>(data, this.valid);
    }
}
