/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * A default integer model implementation.
 */
public final class IntegerModel extends DefaultModel<Integer> {
    /**
     * Creates a new integer model initialized with {@code 0}.
     */
    public IntegerModel() {
    }

    /**
     * Creates a new integer model initialized with the specified value.
     *
     * @param data the initial integer value
     */
    public IntegerModel(final Integer data) {
        super(data);
    }

    @Override
    public Integer getDefaultData() {
        return 0;
    }

    @Override
    public Model<Integer> deriveWithData(final Integer data) {
        return new IntegerModel(data);
    }
}
