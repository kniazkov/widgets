/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.FontSize;

/**
 * A default font size model implementation.
 */
public final class DefaultFontSizeModel extends DefaultModel<FontSize> {
    @Override
    public FontSize getDefaultData() {
        return FontSize.DEFAULT;
    }
}
