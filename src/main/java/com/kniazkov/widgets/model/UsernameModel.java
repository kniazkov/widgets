/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * Defines a string model that is considered invalid if the trimmed string contains spaces.
 */
public final class UsernameModel extends DefaultModel<String> {
    /**
     * Creates a new username model initialized with an empty string.
     */
    public UsernameModel() {
    }

    /**
     * Creates a new username model initialized with the specified value.
     *
     * @param data the initial username value
     */
    public UsernameModel(final String data) {
        super(data);
    }

    @Override
    public boolean isValid() {
        final String value = this.getData().trim();
        return !value.isEmpty() && !value.contains(" ");
    }

    @Override
    protected String getDefaultData() {
        return "";
    }

    @Override
    public Model<String> deriveWithData(final String data) {
        return new UsernameModel(data);
    }
}
