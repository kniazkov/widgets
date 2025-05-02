/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A standard {@link Model} implementation for storing {@link FontFace} values.
 * <p>
 *     This model extends {@link DefaultModel} and accepts any non-null {@code FontFace} as valid.
 *     It is suitable for UI elements that support font customization via reactive models.
 * </p>
 *
 * <p>
 *     The default value returned by {@link #getDefaultData()} is {@link FontFace#DEFAULT},
 *     which may represent a logical system font. This guarantees that {@link #getData()}
 *     always returns a non-null, valid value, even if no specific font face has been set yet.
 * </p>
 */
public final class DefaultFontFaceModel extends DefaultModel<FontFace> {
    @Override
    protected Model<FontFace> createInstance() {
        return new DefaultFontFaceModel();
    }

    @Override
    public FontFace getDefaultData() {
        return FontFace.DEFAULT;
    }
}
