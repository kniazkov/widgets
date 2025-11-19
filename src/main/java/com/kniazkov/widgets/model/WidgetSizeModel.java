/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.WidgetSize;

/**
 * A default widget size model implementation.
 */
public final class WidgetSizeModel extends DefaultModel<WidgetSize> {
    /**
     * Creates a new widget size model initialized with
     * {@link AbsoluteSize#UNDEFINED}.
     */
    public WidgetSizeModel() {
    }

    /**
     * Creates a new absolute size model initialized with the specified value.
     *
     * @param data the initial size
     */
    public WidgetSizeModel(final WidgetSize data) {
        super(data);
    }

    /**
     * Creates a new widget size model initialized from a CSS-style string.
     * The string is parsed according to {@link WidgetSize#parse(String)} (String)},
     * supporting units like {@code px}, {@code pt}, {@code in}, {@code %}, etc.
     *
     * @param data the size string (e.g., "10pt", "24px", "1in", "100%")
     * @throws IllegalArgumentException if the string is invalid
     */
    public WidgetSizeModel(final String data) {
        this(WidgetSize.parse(data));
    }

    @Override
    public WidgetSize getDefaultData() {
        return AbsoluteSize.UNDEFINED;
    }

    @Override
    public Model<WidgetSize> deriveWithData(final WidgetSize data) {
        return new WidgetSizeModel(data);
    }
}
