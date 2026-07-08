/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

/**
 * Defines a string model that is considered valid if it contains a phone number
 * in international format: a plus sign followed by eight to fifteen digits.
 */
public final class PhoneNumberModel extends DefaultModel<String> {
    /**
     * Creates a new phone number model initialized with an empty string.
     */
    public PhoneNumberModel() {
    }

    /**
     * Creates a new phone number model initialized with the specified value.
     *
     * @param data the initial phone number value
     */
    public PhoneNumberModel(final String data) {
        super(data);
    }

    @Override
    public boolean isValid() {
        final String value = this.getData();

        if (value.length() < 9 || value.length() > 16) {
            return false;
        }

        if (value.charAt(0) != '+') {
            return false;
        }

        for (int i = 1; i < value.length(); i++) {
            final char ch = value.charAt(i);

            if (ch < '0' || ch > '9') {
                return false;
            }
        }

        return true;
    }

    @Override
    protected String getDefaultData() {
        return "";
    }

    @Override
    public Model<String> deriveWithData(final String data) {
        return new PhoneNumberModel(data);
    }
}
