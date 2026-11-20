/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A nonempty exact decimal selection on a finite, inclusive and evenly spaced grid.
 */
public final class SliderModel extends SingleThreadModel<BigDecimal> {
    /**
     * Lower bound at the common display scale.
     */
    private final BigDecimal minimum;
    /**
     * Positive increment at the common display scale.
     */
    private final BigDecimal step;
    /**
     * Inclusive last tick.
     */
    private final int ticks;
    /**
     * Current value at the grid scale.
     */
    private BigDecimal value;

    /**
     * @param minimum first value
     * @param maximum last value, strictly greater than minimum
     * @param step positive exact divisor of the range
     * @param value initial value on the grid
     */
    public SliderModel(final BigDecimal minimum, final BigDecimal maximum, final BigDecimal step,
            final BigDecimal value) {
        final BigDecimal low = bounded(minimum);
        final BigDecimal high = bounded(maximum);
        final BigDecimal increment = bounded(step);
        if (high.compareTo(low) <= 0 || increment.signum() <= 0) {
            throw new IllegalArgumentException("Expected increasing bounds and positive step");
        }
        final int scale = Math.max(0, Math.max(low.scale(), increment.scale()));
        this.minimum = low.setScale(scale);
        this.step = increment.setScale(scale);
        final BigDecimal[] parts = high.subtract(low).divideAndRemainder(increment);
        if (parts[1].signum() != 0 || parts[0].compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
            throw new IllegalArgumentException("Expected 1..1000000 whole steps");
        }
        this.ticks = parts[0].intValueExact();
        this.value = this.onGrid(value);
    }

    /**
     * @param number decimal supplied by application code
     * @return decimal in the supported precision range
     */
    private static BigDecimal bounded(final BigDecimal number) {
        Objects.requireNonNull(number);
        if (number.scale() < -18 || number.scale() > 18
                || (long) number.precision() - number.scale() > 18) {
            throw new IllegalArgumentException("Unsupported slider precision or scale");
        }
        return number;
    }

    /**
     * @param candidate requested value
     * @return canonical grid value
     */
    private BigDecimal onGrid(final BigDecimal candidate) {
        final BigDecimal delta = bounded(candidate).subtract(this.minimum);
        final BigDecimal[] parts = delta.divideAndRemainder(this.step);
        if (parts[1].signum() != 0 || parts[0].signum() < 0
                || parts[0].compareTo(BigDecimal.valueOf(this.ticks)) > 0) {
            throw new IllegalArgumentException("Slider value is outside its grid");
        }
        return candidate.setScale(this.step.scale(), RoundingMode.UNNECESSARY);
    }

    /**
     * @return minimum at the common display scale
     */
    public BigDecimal minimum() {
        return this.minimum;
    }

    /**
     * @return increment at the common display scale
     */
    public BigDecimal step() {
        return this.step;
    }

    /**
     * @return last valid zero-based tick
     */
    public int ticks() {
        return this.ticks;
    }

    /**
     * @return selected zero-based tick
     */
    public int index() {
        return this.value.subtract(this.minimum).divideToIntegralValue(this.step).intValueExact();
    }

    /**
     * @param index zero-based tick received from the client
     */
    public void select(final int index) {
        if (index < 0 || index > this.ticks) {
            throw new IllegalArgumentException("Invalid slider tick");
        }
        final BigDecimal next = this.minimum.add(this.step.multiply(BigDecimal.valueOf(index)));
        this.assign(next);
    }

    /**
     * @param next validated canonical value
     * @return whether the selection changed
     */
    private boolean assign(final BigDecimal next) {
        if (this.value.equals(next)) {
            return false;
        }
        this.value = next;
        this.notifyListeners();
        return true;
    }

    @Override
    public boolean setData(final BigDecimal data) {
        return this.assign(this.onGrid(data));
    }

    @Override
    public BigDecimal getData() {
        return this.value;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Model<BigDecimal> deriveWithData(final BigDecimal data) {
        return new SliderModel(this.minimum,
            this.minimum.add(this.step.multiply(BigDecimal.valueOf(this.ticks))), this.step, data);
    }
}
