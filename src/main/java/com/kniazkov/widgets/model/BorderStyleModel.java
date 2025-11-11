/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.BorderStyle;

/**
 * A default border style model implementation.
 */
public final class BorderStyleModel extends DefaultModel<BorderStyle> {
    /**
     * Creates a new border style model initialized with {@link BorderStyle#NONE}.
     */
    public BorderStyleModel() {
    }

    /**
     * Creates a new border style model initialized with the specified value.
     *
     * @param data the initial border style
     */
    public BorderStyleModel(final BorderStyle data) {
        super(data);
    }

    @Override
    public BorderStyle getDefaultData() {
        return BorderStyle.NONE;
    }

    @Override
    public Model<BorderStyle> deriveWithData(final BorderStyle data) {
        return new BorderStyleModel(data);
    }
}
