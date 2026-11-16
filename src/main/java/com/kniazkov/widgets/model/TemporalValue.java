/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Explicit empty or present temporal value, without sentinel dates or public nulls.
 * @param <T> immutable temporal value type
 */
public final class TemporalValue<T> {
    /**
     * Absent only for the explicit empty value.
     */
    private final T value;

    /**
     * Creates an internal value.
     * @param value value or absent internal state
     */
    private TemporalValue(final T value) {
        this.value = value;
    }

    /**
     * @param <T> value type
     * @return empty value
     */
    public static <T> TemporalValue<T> empty() {
        return new TemporalValue<>(null);
    }

    /**
     * @param value non-null value
     * @param <T> value type
     * @return present value
     */
    public static <T> TemporalValue<T> of(final T value) {
        return new TemporalValue<>(Objects.requireNonNull(value));
    }

    /**
     * @return optional immutable value
     */
    public Optional<T> value() {
        return Optional.ofNullable(this.value);
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof TemporalValue<?> temporal
            && Objects.equals(this.value, temporal.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.value);
    }
}
