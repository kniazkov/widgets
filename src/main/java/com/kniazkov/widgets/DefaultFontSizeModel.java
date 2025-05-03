/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A standard {@link Model} implementation for storing {@link FontSize} values.
 * <p>
 *     This model extends {@link DefaultModel} and accepts any non-null {@code FontSize} as valid.
 *     It is intended for use in widgets or styles where font size can be dynamically adjusted.
 * </p>
 *
 * <p>
 *     The default value returned by {@link #getDefaultData()} is {@link FontSize#DEFAULT},
 *     which corresponds to {@code 12pt} (or {@code 16px}).
 *     This guarantees that {@link #getData()} always returns a valid, non-null value,
 *     even if no size has been explicitly set.
 * </p>
 */
public final class DefaultFontSizeModel extends DefaultModel<FontSize> {

    @Override
    protected Model<FontSize> createInstance() {
        return new DefaultFontSizeModel();
    }

    @Override
    public FontSize getDefaultData() {
        return FontSize.DEFAULT;
    }
}
