/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Color;

/**
 * A default color model implementation.
 */
public final class DefaultColorModel extends DefaultModel<Color> {
    @Override
    public Color getDefaultData() {
        return Color.BLACK;
    }
}
