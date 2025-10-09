/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * A default integer model implementation.
 */
public final class DefaultIntegerModel extends DefaultModel<Integer> {
    @Override
    public Integer getDefaultData() {
        return 0;
    }
}
