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
public final class RealToStringModel extends SingleThreadModel<String> {
    /**
     * The underlying double-based model.
     */
    private final Model<Double> base;

    /**
     * A listener that forwards updates from the base model to this wrapper’s listeners.
     */
    private final Listener<Double> forwarder;

    /**
     * The current string representation of the double value.
     */
    private String string;

    /**
     * Indicates whether the current string value is valid (i.e., can be parsed as a double).
     */
    private boolean valid;

    /**
     * Creates a new adapter over the specified real-number model.
     *
     * @param base the base {@code Double}-backed model
     */
    public RealToStringModel(final Model<Double> base) {
        this.base = base;
        this.forwarder = data -> {
            final String value = data.toString();
            if (!this.string.equals(value)) {
                this.string = value;
                this.notifyListeners(value);
            }
        };
        this.string = base.getData().toString();
        this.valid = base.isValid();

        base.addListener(this.forwarder);
    }

    @Override
    public boolean isValid() {
        return this.valid && this.base.isValid();
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
            double value = Double.parseDouble(data);
            this.base.setData(value);
            this.valid = true;
        } catch (NumberFormatException ignored) {
            this.valid = false;
        }

        this.notifyListeners(data);
        return true;
    }

    @Override
    public Model<String> deriveWithData(final String data) {
        return new StringModel(data);
    }
}
