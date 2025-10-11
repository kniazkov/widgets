/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.FontFace;

/**
 * A default font face model implementation.
 */
public final class DefaultFontFaceModel extends DefaultModel<FontFace> {
    /**
     * A {@link ModelFactory} that produces {@link DefaultFontFaceModel} instances.
     */
    public static final ModelFactory<FontFace> FACTORY = DefaultFontFaceModel::new;

    /**
     * Creates a new font face model initialized with {@link FontFace#DEFAULT}.
     */
    public DefaultFontFaceModel() {
    }

    /**
     * Creates a new font face model initialized with the specified value.
     *
     * @param data the initial font face
     */
    public DefaultFontFaceModel(final FontFace data) {
        super(data);
    }

    @Override
    public FontFace getDefaultData() {
        return FontFace.DEFAULT;
    }
}
