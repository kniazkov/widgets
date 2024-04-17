/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

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
        this.valid = true;
    }

    @Override
    public @NotNull String getData() {
        return String.valueOf(this.getIntValue());
    }

    @Override
    protected boolean writeData(@NotNull String data) {
        try {
            this.value = Integer.parseInt(data);
            this.valid = true;
        } catch (NumberFormatException ignored) {
            this.valid = false;
        }
        return this.valid;
    }

    @Override
    public boolean isValid() {
        return this.valid;
    }

    /**
     * Returns the integer value that contains the model.
     * @return Integer value
     */
    public int getIntValue() {
        return this.valid ? this.value : 0;
    }

    /**
     * Sets the new integer value.
     * @param newValue New integer value
     */
    public void setIntValue(final int newValue) {
        if (this.value != newValue || !this.valid) {
            this.value = newValue;
            this.notifyListeners(String.valueOf(newValue));
        }
    }
}
