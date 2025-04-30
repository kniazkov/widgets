/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A standard {@link Model} implementation for storing {@link Color} values.
 * <p>
 *     This model extends {@link DefaultModel} and inherits its no-validation behavior:
 *     any non-null {@code Color} is accepted as valid. It is suitable for use in UI frameworks,
 *     configuration systems, or any context where colors need to be modeled and observed.
 * </p>
 *
 * <p>
 *     The default value returned by {@link #getDefaultData()} is {@link Color#BLACK},
 *     ensuring that {@link #getData()} always returns a non-null, valid color even before
 *     explicit assignment.
 * </p>
 */
public final class DefaultColorModel extends DefaultModel<Color> {
    @Override
    protected Model<Color> createInstance() {
        return new DefaultColorModel();
    }

    @Override
    public Color getDefaultData() {
        return Color.BLACK;
    }
}
