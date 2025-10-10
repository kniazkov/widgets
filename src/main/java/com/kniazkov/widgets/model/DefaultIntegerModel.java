/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * A default integer model implementation.
 */
public final class DefaultIntegerModel extends DefaultModel<Integer> {
    /**
     * Creates a new integer model initialized with {@code 0}.
     */
    public DefaultIntegerModel() {
    }

    /**
     * Creates a new integer model initialized with the specified value.
     *
     * @param data the initial integer value
     */
    public DefaultIntegerModel(final Integer data) {
        super(data);
    }

    @Override
    public Integer getDefaultData() {
        return 0;
    }
}
