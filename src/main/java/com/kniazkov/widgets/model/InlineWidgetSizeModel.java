/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.InlineWidgetSize;

/**
 * A default inline widget size model implementation.
 */
public final class InlineWidgetSizeModel extends DefaultModel<InlineWidgetSize> {
    /**
     * Creates a new inline widget size model initialized with
     * {@link InlineWidgetSize#UNDEFINED}.
     */
    public InlineWidgetSizeModel() {
    }

    /**
     * Creates a new inline widget size model initialized with the specified value.
     *
     * @param data the initial inline widget size
     */
    public InlineWidgetSizeModel(final InlineWidgetSize data) {
        super(data);
    }

    /**
     * Creates a new inline widget size model initialized from a CSS-style string.
     * The string is parsed according to {@link InlineWidgetSize#InlineWidgetSize(String)},
     * supporting units like {@code px}, {@code pt}, {@code in}, etc.
     *
     * @param data the size string (e.g., "10pt", "24px", "1in")
     * @throws IllegalArgumentException if the string is invalid
     */
    public InlineWidgetSizeModel(final String data) {
        this(new InlineWidgetSize(data));
    }

    @Override
    public InlineWidgetSize getDefaultData() {
        return InlineWidgetSize.UNDEFINED;
    }

    @Override
    public Model<InlineWidgetSize> deriveWithData(final InlineWidgetSize data) {
        return new InlineWidgetSizeModel(data);
    }
}
