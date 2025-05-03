/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A standard {@link Model} implementation specialized for {@link Integer} data.
 * <p>
 *     This model inherits behavior from {@link DefaultModel} and performs no validation:
 *     any non-null {@code Integer} is accepted and stored.
 *     It is suitable for numeric values such as counters, sizes, or configuration parameters.
 * </p>
 *
 * <p>
 *     The default value returned by {@link #getDefaultData()} is {@code 0},
 *     ensuring that {@link #getData()} always yields a non-null, valid result
 *     even when no explicit value has been set.
 * </p>
 */
public final class DefaultIntegerModel extends DefaultModel<Integer> {

    @Override
    protected Model<Integer> createInstance() {
        return new DefaultIntegerModel();
    }

    @Override
    public Integer getDefaultData() {
        return 0;
    }
}
