/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.util.Optional;

/**
 * Model that processes text data as an integer.
 */
public final class IntegerModel extends Model<String> {
    /**
     * Integer value.
     */
    private int value;

    /**
     * Whether the value is valid.
     */
    private boolean valid;

    /**
     * Constructor.
     */
    public IntegerModel() {
        this.value = 0;
        this.valid = false;
    }

    @Override
    protected Model<String> createInstance() {
        return new IntegerModel();
    }

    @Override
    public boolean isValid() {
        return this.valid;
    }

    @Override
    protected Optional<String> readData() {
        if (this.valid) {
            return Optional.of(String.valueOf(this.value));
        }
        return Optional.empty();
    }

    @Override
    protected boolean writeData(final String data) {
        try {
            this.value = Integer.parseInt(data);
            this.valid = true;
        } catch (final NumberFormatException ignored) {
            this.valid = false;
        }
        return this.valid;
    }

    @Override
    protected String getDefaultData() {
        return "0";
    }

    public int getIntValue() {
        return this.valid ? this.value : 0;
    }

    public void setIntValue(final int value) {
        this.detach();
        this.value = value;
        this.valid = true;
    }
}
