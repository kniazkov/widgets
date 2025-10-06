/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * A default boolean model implementation.
 */
public final class DefaultBooleanModel extends DefaultModel<Boolean> {
    @Override
    public Boolean getDefaultData() {
        return false;
    }
}
