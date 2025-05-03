/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A standard {@link Model} implementation for storing {@link FontWeight} values.
 * <p>
 *     This model extends {@link DefaultModel} and accepts any non-null {@code FontWeight} as valid.
 *     It is intended for use in widgets or styles where font weight (i.e., text thickness)
 *     can be customized dynamically.
 * </p>
 *
 * <p>
 *     The default value returned by {@link #getDefaultData()} is {@link FontWeight#NORMAL},
 *     which corresponds to the standard font weight ({@code 400}).
 *     This ensures that {@link #getData()} always returns a non-null and valid value,
 *     even if no weight has been explicitly assigned.
 * </p>
 */
public final class DefaultFontWeightModel extends DefaultModel<FontWeight> {

    @Override
    protected Model<FontWeight> createInstance() {
        return new DefaultFontWeightModel();
    }

    @Override
    public FontWeight getDefaultData() {
        return FontWeight.NORMAL;
    }
}
