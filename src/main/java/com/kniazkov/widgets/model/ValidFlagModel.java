/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;

/**
 * A read-only model that exposes the validity flag of another model as a separate {@link Model}.
 * This allows UI components to reactively bind to the validity state of a model —
 * for example, to highlight invalid input fields or disable buttons while data is invalid.
 * The model automatically updates whenever the base model changes, and its own value
 * is always considered valid (since validity itself cannot be invalid).
 *
 * @param <T> the type of data in the base model
 */
public class ValidFlagModel<T> extends ReadOnlyModel<Boolean> {
    /**
     * The wrapped base model.
     */
    private final Model<T> base;

    /**
     * The listener attached to the base model.
     */
    private final Listener<T> listener;

    /**
     * Creates a new validity-flag model based on the specified base model.
     *
     * @param base the model whose validity should be tracked
     */
    public ValidFlagModel(final Model<T> base) {
        this.base = base;
        this.listener = data -> this.notifyListeners(base.isValid());
        base.addListener(this.listener);
    }

    /**
     * Manually detaches this model from the base model.
     * After detachment, this model will no longer receive updates.
     */
    public void detach() {
        this.base.removeListener(this.listener);
    }

    @Override
    public boolean isValid() {
        // The "valid" flag itself is always valid
        return true;
    }

    @Override
    public Boolean getData() {
        return this.base.isValid();
    }

    @Override
    public Boolean getDefaultData() {
        return Boolean.FALSE;
    }
}
