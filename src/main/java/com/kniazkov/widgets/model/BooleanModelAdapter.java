/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;

/**
 * Delegates directly to an arbitrary boolean model without owning subscriptions.
 */
final class BooleanModelAdapter implements BaseBooleanModel {
    /**
     * Original model; writes, validity and notifications remain its responsibility.
     */
    private final Model<Boolean> base;

    /**
     * Creates a view of a non-null source model.
     * @param base source model
     */
    BooleanModelAdapter(final Model<Boolean> base) {
        this.base = base;
    }

    @Override
    public boolean isValid() {
        return this.base.isValid();
    }

    @Override
    public Boolean getData() {
        return this.base.getData();
    }

    @Override
    public boolean setData(final Boolean data) {
        return this.base.setData(data);
    }

    @Override
    public void addListener(final Listener<Boolean> listener) {
        this.base.addListener(listener);
    }

    @Override
    public void removeListener(final Listener<Boolean> listener) {
        this.base.removeListener(listener);
    }

    @Override
    public void notifyListeners() {
        this.base.notifyListeners();
    }

    @Override
    public BaseBooleanModel deriveWithData(final Boolean data) {
        return BaseBooleanModel.of(this.base.deriveWithData(data));
    }
}
