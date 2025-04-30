/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A standard {@link Model} implementation specialized for {@link String} data.
 * <p>
 *     This model inherits behavior from {@link DefaultModel} and thus performs no validation:
 *     any non-null string is accepted and stored. It is ideal for simple text fields, labels,
 *     or any UI element where string data needs to be held and observed.
 * </p>
 *
 * <p>
 *     The default value returned by {@link #getDefaultData()} is the empty string {@code ""},
 *     ensuring that {@link #getData()} always yields a non-null, valid result even when
 *     no explicit value has been set.
 * </p>
 */
public final class DefaultStringModel extends DefaultModel<String> {
    @Override
    protected Model<String> createInstance() {
        return new DefaultStringModel();
    }

    @Override
    public String getDefaultData() {
        return "";
    }
}
