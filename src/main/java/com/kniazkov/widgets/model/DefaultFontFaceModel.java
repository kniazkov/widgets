/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.FontFace;

/**
 * A default font face model implementation.
 */
public final class DefaultFontFaceModel extends DefaultModel<FontFace> {
    @Override
    public FontFace getDefaultData() {
        return FontFace.DEFAULT;
    }
}
