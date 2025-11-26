/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * Defines a string model that is considered invalid if the string is empty.
 */
public final class NotEmptyStringModel extends DefaultModel<String> {
    /**
     * Creates a new invalid string model initialized with an empty string.
     */
    public NotEmptyStringModel() {
    }

    /**
     * Creates a new string model initialized with the specified value.
     *
     * @param data the initial string value
     */
    public NotEmptyStringModel(final String data) {
        super(data);
    }

    @Override
    public boolean isValid() {
        return !this.getData().trim().isEmpty();
    }

    @Override
    protected String getDefaultData() {
        return "";
    }

    @Override
    public Model<String> deriveWithData(final String data) {
        return new NotEmptyStringModel(data);
    }
}
