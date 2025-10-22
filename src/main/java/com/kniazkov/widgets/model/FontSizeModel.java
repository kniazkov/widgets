/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.FontSize;

/**
 * A default font size model implementation.
 */
public final class FontSizeModel extends DefaultModel<FontSize> {
    /**
     * Creates a new font size model initialized with {@link FontSize#DEFAULT}.
     */
    public FontSizeModel() {
    }

    /**
     * Creates a new font size model initialized with the specified value.
     *
     * @param data the initial font size
     */
    public FontSizeModel(final FontSize data) {
        super(data);
    }

    /**
     * Creates a new font size model initialized from a CSS-style string.
     * The string is parsed according to {@link FontSize#FontSize(String)},
     * supporting units like {@code px}, {@code pt}, {@code in}, etc.
     *
     * @param data the font size string (e.g., "12pt", "14px", "1in")
     * @throws IllegalArgumentException if the string is invalid
     */
    public FontSizeModel(String data) {
        this(new FontSize(data));
    }

    @Override
    public FontSize getDefaultData() {
        return FontSize.DEFAULT;
    }

    @Override
    public Model<FontSize> deriveWithData(final FontSize data) {
        return new FontSizeModel(data);
    }
}
