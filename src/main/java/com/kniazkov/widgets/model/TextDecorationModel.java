/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.TextDecoration;

/**
 * Default model for text decoration.
 */
public final class TextDecorationModel extends DefaultModel<TextDecoration> {
    /**
     * Creates a model without a decorative line.
     */
    public TextDecorationModel() {
    }

    /**
     * Creates a model with the specified text decoration.
     *
     * @param data initial value
     */
    public TextDecorationModel(final TextDecoration data) {
        super(data);
    }

    @Override
    protected TextDecoration getDefaultData() {
        return TextDecoration.NONE;
    }

    @Override
    public Model<TextDecoration> deriveWithData(final TextDecoration data) {
        return new TextDecorationModel(data);
    }
}
