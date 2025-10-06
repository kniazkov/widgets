/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.FontWeight;

/**
 * A default font weight model implementation.
 */
public final class DefaultFontWeightModel extends DefaultModel<FontWeight> {
    @Override
    public FontWeight getDefaultData() {
        return FontWeight.NORMAL;
    }
}
