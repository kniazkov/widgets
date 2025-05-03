/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.util.Optional;

/**
 * A model that stores an integer value internally but exposes it as a {@link String}.
 * <p>
 *     This model is useful when working with text-based input fields (e.g., text boxes)
 *     that represent numeric data.
 *     It handles parsing and validation internally: only syntactically valid integer strings
 *     are accepted.
 * </p>
 *
 * <p>
 *     Internally, the model stores an {@code int} value along with a validity flag.
 *     The string representation is dynamically derived from the integer value via
 *     {@link #readData()}.
 * </p>
 *
 * <p>
 *     If an invalid string is provided (e.g., non-numeric input), the value is not updated
 *     and the model remains invalid. The default value returned by {@link #getDefaultData()}
 *     is {@code "0"}, representing the integer zero.
 * </p>
 *
 * <p>
 *     For convenience, {@link #getIntValue()} and {@link #setIntValue(int)} provide direct access
 *     to the numeric value.
 * </p>
 */
public final class IntegerViaStringModel extends Model<String> {
    /**
     * Parsed integer value stored internally.
     */
    private int value;

    /**
     * Flag indicating whether the current value is valid (i.e., successfully parsed).
     */
    private boolean valid;

    /**
     * Constructs an initially invalid integer model with value set to 0.
     */
    public IntegerViaStringModel() {
        this.value = 0;
        this.valid = false;
    }

    @Override
    protected Model<String> createInstance() {
        return new IntegerViaStringModel();
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

    /**
     * Returns the current integer value. If the model is invalid, returns {@code 0}.
     *
     * @return the internal integer value or 0 if invalid
     */
    public int getIntValue() {
        return this.valid ? this.value : 0;
    }

    /**
     * Sets the model's value directly as an integer.
     * <p>
     *     This bypasses string parsing and marks the model as valid. It also detaches the model
     *     from its prototype, if any, as this is considered an authoritative local modification.
     * </p>
     *
     * @param value The integer to set
     */
    public void setIntValue(final int value) {
        this.detach();
        this.valid = true;
        if (this.value != value) {
            this.value = value;
            this.notifyListeners();
        }
    }
}
