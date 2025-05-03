/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A standard {@link Model} implementation specialized for {@link Boolean} data.
 * <p>
 *     This model inherits behavior from {@link DefaultModel} and thus performs no validation:
 *     any non-null {@code Boolean} is accepted and stored.
 *     It is suitable for binary states, such as toggles, visibility, italic flags, etc.
 * </p>
 *
 * <p>
 *     The default value returned by {@link #getDefaultData()} is {@code false},
 *     ensuring that {@link #getData()} always yields a non-null, valid result
 *     even when no explicit value has been set.
 * </p>
 */
public final class DefaultBooleanModel extends DefaultModel<Boolean> {

    @Override
    protected Model<Boolean> createInstance() {
        return new DefaultBooleanModel();
    }

    @Override
    public Boolean getDefaultData() {
        return false;
    }
}
