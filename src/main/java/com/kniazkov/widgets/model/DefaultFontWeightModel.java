/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.FontWeight;

/**
 * A default font weight model implementation.
 */
public final class DefaultFontWeightModel extends DefaultModel<FontWeight> {
    /**
     * A {@link ModelFactory} that produces {@link DefaultFontWeightModel} instances.
     */
    public static final ModelFactory<FontWeight> FACTORY = DefaultFontWeightModel::new;

    /**
     * Creates a new font weight model initialized with {@link FontWeight#NORMAL}.
     */
    public DefaultFontWeightModel() {
    }

    /**
     * Creates a new font weight model initialized with the specified value.
     *
     * @param data the initial font weight
     */
    public DefaultFontWeightModel(final FontWeight data) {
        super(data);
    }

   @Override
    public FontWeight getDefaultData() {
        return FontWeight.NORMAL;
    }
}
