/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;

/**
 * A model adapter that converts an {@link Integer}-based model to a {@link String}-based one.
 * <p>
 * This model provides a textual representation of an integer value, making it suitable for
 * text input fields that display numbers as strings. It keeps the base integer model
 * and the string representation synchronized in both directions.
 * </p>
 *
 * <p>
 * When the base model changes, this adapter updates its own string value and notifies listeners.
 * When the string changes, it tries to parse the value as an integer and write it back
 * to the base model. If parsing fails, the adapter becomes invalid until a valid integer string
 * is provided again.
 * </p>
 */
public final class IntegerToStringModel extends SingleThreadModel<String>
        implements Listener<Integer> {
    /**
     * The underlying integer-based model.
     */
    private final Model<Integer> base;

    /**
     * The current string representation of the integer value.
     */
    private String string;

    /**
     * Indicates whether the current string value is valid (i.e., can be parsed as an integer).
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
     * Creates a new adapter over the specified integer model.
     *
     * @param base the base integer model
     */
    public IntegerToStringModel(final Model<Integer> base) {
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
            final int value = Integer.parseInt(data);
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
    public void accept(final Integer data) {
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
