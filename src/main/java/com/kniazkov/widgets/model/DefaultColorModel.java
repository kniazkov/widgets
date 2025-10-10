/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Color;

/**
 * A default color model implementation.
 */
public final class DefaultColorModel extends DefaultModel<Color> {
    /**
     * Creates a new color model initialized with {@link Color#BLACK}.
     */
    public DefaultColorModel() {
    }

    /**
     * Creates a new color model initialized with the specified value.
     *
     * @param data the initial color
     */
    public DefaultColorModel(final Color data) {
        super(data);
    }

    @Override
    public Color getDefaultData() {
        return Color.BLACK;
    }
}
