/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Exact decimal editor. Raw drafts are retained; invalid data never exposes an older value.
 * Values have at most 256 digits and scale 0..64; wire text never uses floating point.
 */
public final class NumberModel extends SingleThreadModel<NumberValue> {
    /**
     * Explicit validation reason; EMPTY is valid unless the containing form requires a value.
     */
    public enum Status {
        /**
         * Parsed value satisfies all constraints.
         */
        VALID,
        /**
         * Deliberately empty editor.
         */
        EMPTY,
        /**
         * Malformed or unfinished input, including a lone sign or trailing separator.
         */
        CONVERSION,
        /**
         * Resource limit exceeded without rounding or overflow.
         */
        PRECISION,
        /**
         * Value lies outside inclusive bounds.
         */
        BOUNDS,
        /**
         * Value is not on the configured step grid.
         */
        STEP
    }

    /**
     * Whether only integral input is accepted.
     */
    private final boolean integer;
    /**
     * Raw editor text, including unfinished input.
     */
    private String raw = "";
    /**
     * Locale determines the single accepted decimal separator; grouping is never accepted.
     */
    private Locale locale = Locale.US;
    /**
     * Absent internal lower bound.
     */
    private BigDecimal minimum;
    /**
     * Absent internal upper bound.
     */
    private BigDecimal maximum;
    /**
     * Positive exact grid spacing, relative to minimum or zero.
     */
    private BigDecimal step;
    /**
     * Owned adapter shares validity and notifications with the typed model.
     */
    private final Model<String> text = new SingleThreadModel<>() {
        @Override
        public String getData() {
            return NumberModel.this.raw;
        }
        @Override
        public boolean isValid() {
            return NumberModel.this.isValid();
        }
        @Override
        public boolean setData(final String value) {
            Objects.requireNonNull(value);
            if (value.equals(NumberModel.this.raw)) {
                return false;
            }
            NumberModel.this.raw = value;
            NumberModel.this.changed();
            return true;
        }
        @Override
        public Model<String> deriveWithData(final String value) {
            final NumberModel copy = NumberModel.this.copy();
            copy.text.setData(value);
            return copy.text;
        }
    };

    /**
     * @param integer whether fractional input is prohibited
     */
    private NumberModel(final boolean integer) {
        this.integer = integer;
        this.step = integer ? BigDecimal.ONE : new BigDecimal("0.01");
    }

    /**
     * @return empty integer model with step one
     */
    public static NumberModel integers() {
        return new NumberModel(true);
    }

    /**
     * @return empty decimal model with step 0.01; change step explicitly for other precision
     */
    public static NumberModel decimals() {
        return new NumberModel(false);
    }

    /**
     * @return whether only integer syntax is accepted
     */
    public boolean isInteger() {
        return this.integer;
    }

    /**
     * @return raw editable adapter; application code can inspect status separately
     */
    public Model<String> textModel() {
        return this.text;
    }

    /**
     * @return configured locale
     */
    public Locale getLocale() {
        return this.locale;
    }

    /**
     * Changes the separator of valid text exactly, without grouping or rounding.
     * Invalid drafts must be corrected or cleared first, so a locale cannot reinterpret them.
     * @param value locale with a dot or comma decimal separator
     */
    public void setLocale(final Locale value) {
        final char separator = DecimalFormatSymbols.getInstance(Objects.requireNonNull(value))
            .getDecimalSeparator();
        if (value.getLanguage().isEmpty() || (separator != '.' && separator != ',')) {
            throw new IllegalArgumentException("Only dot and comma decimal locales are supported");
        }
        if (!this.isValid()) {
            throw new IllegalStateException("Correct the numeric draft before changing locale");
        }
        final String next = this.raw.replace(this.separator(), separator);
        this.locale = value;
        this.raw = next;
        this.changed();
    }

    /**
     * @return locale decimal separator
     */
    private char separator() {
        return DecimalFormatSymbols.getInstance(this.locale).getDecimalSeparator();
    }

    /**
     * @return precise reason for an invalid value; no parser diagnostic is exposed
     */
    public Status status() {
        if (this.raw.isEmpty()) {
            return Status.EMPTY;
        }
        if (this.raw.length() > 512) {
            return Status.PRECISION;
        }
        final String canonical = this.raw.replace(this.separator(), '.');
        if ((this.separator() != '.' && this.raw.indexOf('.') >= 0)
                || !canonical.matches(this.integer ? "-?[0-9]+" : "-?[0-9]+(\\.[0-9]+)?")) {
            return Status.CONVERSION;
        }
        final BigDecimal value = new BigDecimal(canonical);
        if (!representable(value)) {
            return Status.PRECISION;
        }
        return this.validate(value);
    }

    /**
     * @param value already parsed number
     * @return whether its representation fits the documented resource budget
     */
    private static boolean representable(final BigDecimal value) {
        return value.scale() >= 0 && value.scale() <= 64 && value.precision() <= 256;
    }

    /**
     * @param value parsed number
     * @return bounds or step violation, or VALID
     */
    private Status validate(final BigDecimal value) {
        if ((this.minimum != null && value.compareTo(this.minimum) < 0)
                || (this.maximum != null && value.compareTo(this.maximum) > 0)) {
            return Status.BOUNDS;
        }
        final BigDecimal base = this.minimum == null ? BigDecimal.ZERO : this.minimum;
        return value.subtract(base).remainder(this.step).signum() == 0 ? Status.VALID : Status.STEP;
    }

    /**
     * @return exact valid value, absent for empty or invalid drafts
     */
    public Optional<BigDecimal> value() {
        return this.status() == Status.VALID
            ? Optional.of(new BigDecimal(this.raw.replace(this.separator(), '.')))
            : Optional.empty();
    }

    @Override
    public boolean isValid() {
        final Status state = this.status();
        return state == Status.EMPTY || state == Status.VALID;
    }

    @Override
    public NumberValue getData() {
        return this.value().map(NumberValue::of).orElseGet(NumberValue::empty);
    }

    @Override
    public boolean setData(final NumberValue value) {
        final BigDecimal number = Objects.requireNonNull(value).value().orElse(null);
        if (number == null) {
            return this.text.setData("");
        }
        this.checkNumber(number);
        if (this.validate(number) != Status.VALID) {
            throw new IllegalArgumentException("Number violates bounds or step");
        }
        return this.text.setData(number.toPlainString().replace('.', this.separator()));
    }

    /**
     * @param value finite decimal fitting the configured mode
     */
    private void checkNumber(final BigDecimal value) {
        Objects.requireNonNull(value);
        if (!representable(value) || (this.integer && value.scale() != 0)) {
            throw new IllegalArgumentException("Unsupported numeric precision");
        }
    }

    /**
     * Changes all constraints atomically and revalidates the draft without clamping it.
     * Grid origin is the lower bound when present, otherwise zero.
     * @param lower empty for unbounded
     * @param upper empty for unbounded
     * @param increment strictly positive exact step
     */
    public void configure(final NumberValue lower, final NumberValue upper,
            final BigDecimal increment) {
        final BigDecimal min = Objects.requireNonNull(lower).value().orElse(null);
        final BigDecimal max = Objects.requireNonNull(upper).value().orElse(null);
        if (min != null) {
            this.checkNumber(min);
        }
        if (max != null) {
            this.checkNumber(max);
        }
        this.checkNumber(increment);
        if (increment.signum() <= 0 || (min != null && max != null && min.compareTo(max) > 0)) {
            throw new IllegalArgumentException("Invalid numeric constraints");
        }
        this.minimum = min;
        this.maximum = max;
        this.step = increment;
        this.changed();
    }

    /**
     * Computes a permitted adjacent value; invalid drafts and off-grid bounds never clamp.
     * Empty input starts at the lower bound or zero, only if that candidate is in range.
     * @param direction exactly -1 or 1
     * @return next valid value, or absent at a boundary or for invalid input
     */
    public Optional<BigDecimal> next(final int direction) {
        if (direction != -1 && direction != 1) {
            throw new IllegalArgumentException("Direction must be -1 or 1");
        }
        if (!this.isValid()) {
            return Optional.empty();
        }
        final BigDecimal next;
        if (this.raw.isEmpty()) {
            next = this.minimum != null ? this.minimum : BigDecimal.ZERO;
        } else {
            next = this.value().orElseThrow()
                .add(this.step.multiply(BigDecimal.valueOf(direction)));
        }
        return representable(next) && this.validate(next) == Status.VALID
            ? Optional.of(next) : Optional.empty();
    }

    /**
     * Both buttons and keyboard actions use the same checked mutation as programmatic writes.
     * @param direction exactly -1 or 1
     * @return whether the value changed
     */
    public boolean move(final int direction) {
        return this.next(direction).map(value -> this.setData(NumberValue.of(value))).orElse(false);
    }

    /**
     * Notifies both value and validity consumers, including changes to constraints alone.
     */
    private void changed() {
        this.text.notifyListeners();
        this.notifyListeners();
    }

    /**
     * @return independent empty model retaining constraints and locale
     */
    private NumberModel copy() {
        final NumberModel copy = new NumberModel(this.integer);
        copy.minimum = this.minimum;
        copy.maximum = this.maximum;
        copy.step = this.step;
        copy.locale = this.locale;
        return copy;
    }

    @Override
    public Model<NumberValue> deriveWithData(final NumberValue value) {
        final NumberModel copy = this.copy();
        copy.setData(value);
        return copy;
    }
}
