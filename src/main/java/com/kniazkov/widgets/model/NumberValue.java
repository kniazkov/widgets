/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Explicit empty or present decimal value, without sentinel numbers or public nulls.
 */
public final class NumberValue {
    /**
     * Absent only for the explicit empty value.
     */
    private final java.math.BigDecimal value;

    /**
     * Creates an internal value.
     * @param value value or absent internal state
     */
    private NumberValue(final java.math.BigDecimal value) {
        this.value = value;
    }

    /**
     * @return empty value
     */
    public static NumberValue empty() {
        return new NumberValue(null);
    }

    /**
     * @param value non-null value
     * @return present value
     */
    public static NumberValue of(final java.math.BigDecimal value) {
        return new NumberValue(Objects.requireNonNull(value));
    }

    /**
     * @return optional immutable value
     */
    public Optional<java.math.BigDecimal> value() {
        return Optional.ofNullable(this.value);
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof NumberValue numeric
            && Objects.equals(this.value, numeric.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.value);
    }
}
