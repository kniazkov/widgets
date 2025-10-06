/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import java.util.Optional;

/**
 * A {@link Model} that bridges between an integer value and its string representation.
 * This model allows UI elements that operate on text (like {@code InputField})
 * to be bound to integer values transparently. The model automatically converts
 * between {@code int} and {@code String} representations.
 * <ul>
 *   <li>Parsing errors (non-numeric input) mark the model as invalid.</li>
 *   <li>When invalid, {@link #getData()} returns the the default ("0").</li>
 *   <li>Setting an integer value triggers a listener update with its string form.</li>
 * </ul>
 */
public final class IntegerToStringModel extends Model<String> {
    /**
     * The current integer value represented by this model.
     */
    private int intValue = 0;

    /**
     * Indicates whether the current value is valid (i.e. successfully parsed).
     */
    private boolean valid = true;

    /**
     * Returns the current integer value.
     *
     * @return the integer value
     */
    public int getIntValue() {
        return this.intValue;
    }

    /**
     * Sets a new integer value.
     *
     * @param value the new integer value to set
     */
    public void setIntValue(final int value) {
        if (this.intValue != value || !this.valid) {
            this.intValue = value;
            this.valid = true;
            this.notifyListeners(String.valueOf(value));
        }
    }

    @Override
    public boolean isValid() {
        return this.valid;
    }

    @Override
    protected Optional<String> readData() {
        return Optional.of(String.valueOf(this.intValue));
    }

    @Override
    protected String getDefaultData() {
        return "0";
    }

    @Override
    protected boolean writeData(final String data) {
        try {
            this.intValue = Integer.parseInt(data);
            this.valid = true;
            return true;
        } catch (NumberFormatException ignored) {
            this.valid = false;
            return false;
        }
    }
}
