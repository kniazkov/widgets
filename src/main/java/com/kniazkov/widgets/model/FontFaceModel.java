/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.FontFace;

/**
 * A default font face model implementation.
 */
public final class FontFaceModel extends DefaultModel<FontFace> {
    /**
     * Creates a new font face model initialized with {@link FontFace#DEFAULT}.
     */
    public FontFaceModel() {
    }

    /**
     * Creates a new font face model initialized with the specified value.
     *
     * @param data the initial font face
     */
    public FontFaceModel(final FontFace data) {
        super(data);
    }

    @Override
    public FontFace getDefaultData() {
        return FontFace.DEFAULT;
    }

    @Override
    public Model<FontFace> deriveWithData(final FontFace data) {
        return new FontFaceModel(data);
    }
}
