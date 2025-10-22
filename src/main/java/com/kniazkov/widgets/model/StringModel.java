/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * A default string model implementation.
 */
public final class StringModel extends DefaultModel<String> {
    /**
     * Creates a new string model initialized with an empty string.
     */
    public StringModel() {
    }

    /**
     * Creates a new string model initialized with the specified value.
     *
     * @param data the initial string value
     */
    public StringModel(final String data) {
        super(data);
    }

    @Override
    public String getDefaultData() {
        return "";
    }

    @Override
    public Model<String> deriveWithData(final String data) {
        return new StringModel(data);
    }
}
