/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Typed temporal data and its canonical editor text share one validity state.
 * Invalid input is retained as text; typed data becomes empty instead of retaining an old value.
 * @param <T> comparable calendar value, never an implicit instant
 */
public final class TemporalModel<T extends Comparable<? super T>>
        extends SingleThreadModel<TemporalValue<T>> {
    /**
     * Strict parser.
     */
    private final Function<String, T> parser;
    /**
     * Canonical formatter.
     */
    private final Function<T, String> formatter;
    /**
     * Raw input, including invalid input and explicit empty text.
     */
    private String raw = "";
    /**
     * Inclusive lower bound, absent internally when unbounded.
     */
    private T minimum;
    /**
     * Inclusive upper bound, absent internally when unbounded.
     */
    private T maximum;
    /**
     * Owned adapter used by InputField and FormField.
     */
    private final Model<String> text = new SingleThreadModel<>() {
        @Override
        public boolean isValid() {
            return TemporalModel.this.isValid();
        }
        @Override
        public String getData() {
            return TemporalModel.this.raw;
        }
        @Override
        public boolean setData(final String value) {
            Objects.requireNonNull(value);
            if (value.equals(TemporalModel.this.raw)) {
                return false;
            }
            TemporalModel.this.raw = value;
            TemporalModel.this.changed();
            return true;
        }
        @Override
        public Model<String> deriveWithData(final String value) {
            final TemporalModel<T> copy = TemporalModel.this.copy();
            copy.text.setData(value);
            return copy.text;
        }
    };

    /**
     * Creates an empty model with explicit parsing and formatting rules.
     * @param parser strict canonical parser
     * @param formatter canonical formatter
     */
    private TemporalModel(final Function<String, T> parser, final Function<T, String> formatter) {
        this.parser = parser;
        this.formatter = formatter;
    }

    /**
     * @return empty date model supporting years 0001 through 9999
     */
    public static TemporalModel<LocalDate> dates() {
        return new TemporalModel<>(text -> {
            if (!text.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
                throw new IllegalArgumentException("Invalid canonical date");
            }
            final LocalDate date = LocalDate.parse(text);
            if (date.getYear() < 1) {
                throw new IllegalArgumentException("Year must be positive");
            }
            return date;
        }, LocalDate::toString);
    }

    /**
     * @return empty time model with minute precision and no timezone
     */
    public static TemporalModel<LocalTime> times() {
        final DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);
        return new TemporalModel<>(text -> {
            if (!text.matches("[0-9]{2}:[0-9]{2}")) {
                throw new IllegalArgumentException("Invalid canonical time");
            }
            return LocalTime.parse(text, format);
        }, value -> {
            if (value.getSecond() != 0 || value.getNano() != 0) {
                throw new IllegalArgumentException("Time requires minute precision");
            }
            return format.format(value);
        });
    }

    /**
     * @return raw editor model; validity includes parsing and bounds
     */
    public Model<String> textModel() {
        return this.text;
    }

    /**
     * @return typed value only when valid and nonempty
     */
    public Optional<T> value() {
        return this.getData().value();
    }

    @Override
    public boolean isValid() {
        return this.raw.isEmpty() || this.parse().isPresent();
    }

    @Override
    public TemporalValue<T> getData() {
        return this.parse().map(TemporalValue::of).orElseGet(TemporalValue::empty);
    }

    @Override
    public boolean setData(final TemporalValue<T> value) {
        final String next = Objects.requireNonNull(value).value().map(this.formatter).orElse("");
        if (!next.isEmpty()) {
            final T parsed = this.parser.apply(next);
            if (!this.inBounds(parsed)) {
                throw new IllegalArgumentException("Temporal value outside bounds");
            }
        }
        return this.text.setData(next);
    }

    /**
     * Updates both inclusive bounds atomically without changing the user's input.
     * @param lower empty for no lower bound
     * @param upper empty for no upper bound
     */
    public void setBounds(final TemporalValue<T> lower, final TemporalValue<T> upper) {
        final T min = this.checkBound(lower);
        final T max = this.checkBound(upper);
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Reversed temporal bounds");
        }
        this.minimum = min;
        this.maximum = max;
        this.changed();
    }

    /**
     * @return canonical lower bound, or empty
     */
    public String minimumText() {
        return this.minimum == null ? "" : this.formatter.apply(this.minimum);
    }

    /**
     * @return canonical upper bound, or empty
     */
    public String maximumText() {
        return this.maximum == null ? "" : this.formatter.apply(this.maximum);
    }

    /**
     * Validates a bound before any mutation.
     * @param value optional bound
     * @return internal bound or null
     */
    private T checkBound(final TemporalValue<T> value) {
        return Objects.requireNonNull(value).value()
            .map(item -> this.parser.apply(this.formatter.apply(item))).orElse(null);
    }

    /**
     * @return parsed bounded value, absent for empty or invalid raw input
     */
    private Optional<T> parse() {
        if (this.raw.isEmpty()) {
            return Optional.empty();
        }
        try {
            final T value = this.parser.apply(this.raw);
            return this.inBounds(value) ? Optional.of(value) : Optional.empty();
        } catch (final IllegalArgumentException | java.time.DateTimeException invalid) {
            return Optional.empty();
        }
    }

    /**
     * @param value parsed value
     * @return whether both inclusive bounds hold
     */
    private boolean inBounds(final T value) {
        return (this.minimum == null || value.compareTo(this.minimum) >= 0)
            && (this.maximum == null || value.compareTo(this.maximum) <= 0);
    }

    /**
     * Publishes even validity-only changes to both typed and raw consumers.
     */
    private void changed() {
        this.text.notifyListeners();
        this.notifyListeners();
    }

    /**
     * @return empty independent model with the same constraints
     */
    private TemporalModel<T> copy() {
        final TemporalModel<T> copy = new TemporalModel<>(this.parser, this.formatter);
        copy.minimum = this.minimum;
        copy.maximum = this.maximum;
        return copy;
    }

    @Override
    public Model<TemporalValue<T>> deriveWithData(final TemporalValue<T> value) {
        final TemporalModel<T> copy = this.copy();
        copy.setData(value);
        return copy;
    }
}
