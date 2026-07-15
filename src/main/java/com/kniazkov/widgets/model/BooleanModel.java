/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * A default boolean model implementation.
 */
public final class BooleanModel extends DefaultModel<Boolean> {
    /**
     * Creates a new boolean model initialized with {@code false}.
     */
    public BooleanModel() {
    }

    /**
     * Creates a new boolean model initialized with the specified value.
     *
     * @param data the initial boolean value
     */
    public BooleanModel(final Boolean data) {
        super(data);
    }

    @Override
    public Boolean getDefaultData() {
        return false;
    }

    @Override
    public Model<Boolean> deriveWithData(final Boolean data) {
        return new BooleanModel(data);
    }

    /**
     * Returns a model that represents the logical negation of this model’s boolean value.
     *
     * @return a new model exposing {@code !getData()}
     */
    public Model<Boolean> invert() {
        return new InvertModel(this);
    }
}
