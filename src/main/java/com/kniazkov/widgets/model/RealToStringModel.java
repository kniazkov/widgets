/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;

/**
 * A model adapter that converts a {@link Double}-based model to a {@link String}-based one.
 * <p>
 * This model provides a textual representation of a real (double-precision) number,
 * keeping the base model and the string representation synchronized in both directions.
 * </p>
 *
 * <p>
 * When the base model changes, this adapter updates its own string value and notifies listeners.
 * When the string changes, it attempts to parse it as a Double. If parsing succeeds, the new
 * numeric value is written back to the base model. If parsing fails, the adapter becomes invalid
 * until a valid floating-point string is provided again.
 * </p>
 */
public final class RealToStringModel extends SingleThreadModel<String>
        implements Listener<Double> {
    /**
     * The underlying double-based model.
     */
    private final Model<Double> base;

    /**
     * The current string representation of the double value.
     */
    private String string;

    /**
     * Indicates whether the current string value is valid (i.e., can be parsed as a double).
     */
    private boolean valid;

    /**
     * Last validity state reported by the base model.
     */
    private boolean baseValid;

    /**
     * Prevents the synchronous callback caused by {@link #setData(String)} from overwriting the
     * exact text supplied by the caller.
     */
    private boolean updatingBase;

    /**
     * Creates a new adapter over the specified real-number model.
     *
     * @param base the base {@code Double}-backed model
     */
    public RealToStringModel(final Model<Double> base) {
        this.base = base;
        this.string = base.getData().toString();
        this.valid = true;
        this.baseValid = base.isValid();
        this.base.addListener(this);
    }

    @Override
    public boolean isValid() {
        return this.valid && this.baseValid;
    }

    @Override
    public String getData() {
        return this.string;
    }

    @Override
    public boolean setData(final String data) {
        if (this.string.equals(data)) {
            return false;
        }
        this.string = data;

        try {
            final double value = Double.parseDouble(data);
            this.valid = true;
            this.updatingBase = true;
            try {
                this.base.setData(value);
            } finally {
                this.updatingBase = false;
            }
        } catch (NumberFormatException ignored) {
            this.valid = false;
        }

        this.baseValid = this.base.isValid();
        this.notifyListeners(data);
        return true;
    }

    @Override
    public Model<String> deriveWithData(final String data) {
        return new StringModel(data);
    }

    @Override
    public void accept(final Double data) {
        if (this.updatingBase) {
            return;
        }
        final boolean oldValidity = this.isValid();
        final String value = data.toString();
        final boolean changed = !this.string.equals(value);
        this.string = value;
        this.valid = true;
        this.baseValid = this.base.isValid();
        if (changed || oldValidity != this.isValid()) {
            this.notifyListeners(value);
        }
    }
}
