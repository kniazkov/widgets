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
public class InvertModel extends SingleThreadModel<Boolean> {
    /**
     * The wrapped base model.
     */
    private final Model<Boolean> base;

    /**
     * A listener that forwards updates from the base model to this wrapper’s listeners.
     * It must be stored in a field, not in a local variable, so that the garbage collector
     * does not remove the listener from the base model while this wrapper exists.
     */
    private final Listener<Boolean> forwarder;

    /**
     * Creates a new inverted boolean model that reflects the logical negation
     * of the specified base model.
     *
     * @param base the model whose boolean value is to be inverted and exposed
     *  through this wrapper
     */
    public InvertModel(final Model<Boolean> base) {
        this.base = base;
        this.forwarder = data -> this.notifyListeners(!base.getData());

        base.addListener(this.forwarder);
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
}
