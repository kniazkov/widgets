/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;

/**
 * A boolean model wrapper that exposes the logical negation of another boolean-based model.
 * <p>
 * This is particularly useful in UI scenarios where a flag must be inverted.
 * For example, a validation flag can be inverted and used to control a widget’s
 * disabled state.
 */
public class InvertModel extends SingleThreadModel<Boolean> implements Listener<Boolean> {
    /**
     * The wrapped base model.
     */
    private final Model<Boolean> base;

    /**
     * Creates a new inverted boolean model that reflects the logical negation
     * of the specified base model.
     *
     * @param base the model whose boolean value is to be inverted and exposed
     *  through this wrapper
     */
    @SuppressWarnings("this-escape")
    public InvertModel(final Model<Boolean> base) {
        /*
         * Construction registers the derived model before it is returned to the caller.
         */
        this.base = base;
        this.base.addListener(this);
    }

    @Override
    public boolean isValid() {
        return base.isValid();
    }

    @Override
    public Boolean getData() {
        return !base.getData();
    }

    @Override
    public boolean setData(final Boolean data) {
        return base.setData(!data);
    }

    @Override
    public Model<Boolean> deriveWithData(final Boolean data) {
        return new BooleanModel(data);
    }

    @Override
    public void accept(final Boolean data) {
        this.notifyListeners(!data);
    }
}
