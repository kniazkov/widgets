/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Overflow;

/**
 * Default model for overflow behavior.
 */
public final class OverflowModel extends DefaultModel<Overflow> {
    /**
     * Creates a visible-overflow model.
     */
    public OverflowModel() {
    }

    /**
     * Creates a model with an overflow value.
     *
     * @param data initial value
     */
    public OverflowModel(final Overflow data) {
        super(data);
    }

    @Override
    protected Overflow getDefaultData() {
        return Overflow.VISIBLE;
    }

    @Override
    public Model<Overflow> deriveWithData(final Overflow data) {
        return new OverflowModel(data);
    }
}
