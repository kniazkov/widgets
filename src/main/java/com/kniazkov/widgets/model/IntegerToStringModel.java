/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import java.util.Optional;

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
 * When the string changes, it tries to parse the value as an integer and write it back to the base model.
 * If parsing fails, the adapter becomes invalid until a valid integer string is provided again.
 * </p>
 */
public final class IntegerToStringModel extends Model<String> {
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
     * Creates a new adapter over the specified integer model.
     *
     * @param base the base integer model
     */
    public IntegerToStringModel(final Model<Integer> base) {
        this.base = base;
        this.string = base.getData().toString();
        this.valid = base.isValid();

        base.addListener(data -> {
            final String value = data.toString();
            if (!this.string.equals(value)) {
                this.string = value;
                this.notifyListeners();
            }
        });
    }

    @Override
    public boolean isValid() {
        return this.valid;
    }

    @Override
    protected Optional<String> readData() {
        return Optional.of(this.string);
    }

    @Override
    protected String getDefaultData() {
        return this.base.getDefaultData().toString();
    }

    @Override
    protected boolean writeData(final String data) {
        this.string = data;
        try {
            int value = Integer.parseInt(data);
            this.base.setData(value);
            this.valid = true;
        } catch (NumberFormatException ignored) {
            this.valid = false;
        }
        return true;
    }
}
