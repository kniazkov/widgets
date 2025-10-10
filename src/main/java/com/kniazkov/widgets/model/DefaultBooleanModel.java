/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * A default boolean model implementation.
 */
public final class DefaultBooleanModel extends DefaultModel<Boolean> {
    /**
     * Creates a new boolean model initialized with {@code false}.
     */
    public DefaultBooleanModel() {
    }

    /**
     * Creates a new boolean model initialized with the specified value.
     *
     * @param data the initial boolean value
     */
    public DefaultBooleanModel(final Boolean data) {
        super(data);
    }

    @Override
    public Boolean getDefaultData() {
        return false;
    }
}
