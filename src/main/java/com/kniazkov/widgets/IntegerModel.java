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
    private int value = 0;

    @Override
    public @NotNull String getData() {
        return String.valueOf(this.value);
    }

    @Override
    protected boolean writeData(@NotNull String data) {
        try {
            this.value = Integer.parseInt(data);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    /**
     * Returns the integer value that contains the model.
     * @return Integer value
     */
    public int getIntValue() {
        return this.value;
    }

    /**
     * Sets the new integer value.
     * @param newValue New integer value
     */
    public void setIntValue(final int newValue) {
        if (this.value != newValue) {
            this.value = newValue;
            this.notify(String.valueOf(newValue));
        }
    }
}
