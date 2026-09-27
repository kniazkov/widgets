/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.NumericRange;

/**
 * Reactive closed interval; the default is the point [0, 0].
 */
public final class NumericRangeModel extends DefaultModel<NumericRange> {
    /**
     * Creates the zero-point model.
     */
    public NumericRangeModel() { }

    /**
     * @param value initial interval
     */
    public NumericRangeModel(final NumericRange value) {
        super(value);
    }

    @Override public NumericRange getDefaultData() {
        return new NumericRange(0, 0);
    }

    @Override public Model<NumericRange> deriveWithData(final NumericRange value) {
        return new NumericRangeModel(value);
    }
}
