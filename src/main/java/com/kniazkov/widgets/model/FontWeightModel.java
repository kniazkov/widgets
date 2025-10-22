/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.FontWeight;

/**
 * A default font weight model implementation.
 */
public final class FontWeightModel extends DefaultModel<FontWeight> {
    /**
     * Creates a new font weight model initialized with {@link FontWeight#NORMAL}.
     */
    public FontWeightModel() {
    }

    /**
     * Creates a new font weight model initialized with the specified value.
     *
     * @param data the initial font weight
     */
    public FontWeightModel(final FontWeight data) {
        super(data);
    }

   @Override
    public FontWeight getDefaultData() {
        return FontWeight.NORMAL;
    }

    @Override
    public Model<FontWeight> deriveWithData(final FontWeight data) {
        return new FontWeightModel(data);
    }
}
