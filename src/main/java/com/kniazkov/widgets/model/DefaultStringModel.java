/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * A default string model implementation.
 */
public final class DefaultStringModel extends DefaultModel<String> {
    /**
     * Creates a new string model initialized with an empty string.
     */
    public DefaultStringModel() {
    }

    /**
     * Creates a new string model initialized with the specified value.
     *
     * @param data the initial string value
     */
    public DefaultStringModel(final String data) {
        super(data);
    }

    @Override
    public String getDefaultData() {
        return "";
    }
}
