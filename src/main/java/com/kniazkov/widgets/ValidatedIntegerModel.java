/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.util.Optional;

/**
 * A {@link Model} implementation for storing and validating {@link Integer} values.
 * <p>
 *     This model enforces a valid numeric range, rejecting values that fall outside
 *     the configured {@code min} and {@code max} bounds. A default value is provided
 *     and used when no valid value is set or inherited.
 * </p>
 */
public final class ValidatedIntegerModel extends Model<Integer> {

    /**
     * Minimum allowed integer value (inclusive).
     */
    private final int min;

    /**
     * Maximum allowed integer value (inclusive).
     */
    private final int max;

    /**
     * Default fallback value, used when no valid value is set or inherited.
     */
    private final int defaultValue;

    /**
     * Current stored value.
     */
    private int value;

    /**
     * Indicates whether the current value is valid.
     */
    private boolean valid;

    /**
     * Constructs a validated integer model with specified range and default value.
     *
     * @param min The minimum allowed value (inclusive)
     * @param max The maximum allowed value (inclusive)
     * @param defaultValue The fallback value when no valid value is set
     */
    public ValidatedIntegerModel(int min, int max, int defaultValue) {
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.valid = false;
    }

    @Override
    protected Model<Integer> createInstance() {
        return new ValidatedIntegerModel(this.min, this.max, this.defaultValue);
    }

    @Override
    public boolean isValid() {
        return this.valid;
    }

    @Override
    protected Optional<Integer> readData() {
        return valid ? Optional.of(value) : Optional.empty();
    }

    @Override
    protected boolean writeData(final Integer data) {
        if (data >= min && data <= max) {
            this.value = data;
            this.valid = true;
            return true;
        }
        return false;
    }

    @Override
    protected Integer getDefaultData() {
        return this.defaultValue;
    }
}
