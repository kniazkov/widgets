/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.FontSize;

/**
 * A default font size model implementation.
 */
public final class DefaultFontSizeModel extends DefaultModel<FontSize> {
    /**
     * A {@link ModelFactory} that produces {@link DefaultFontSizeModel} instances.
     */
    public static final ModelFactory<FontSize> FACTORY = DefaultFontSizeModel::new;

    /**
     * Creates a new font size model initialized with {@link FontSize#DEFAULT}.
     */
    public DefaultFontSizeModel() {
    }

    /**
     * Creates a new font size model initialized with the specified value.
     *
     * @param data the initial font size
     */
    public DefaultFontSizeModel(final FontSize data) {
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
    public DefaultFontSizeModel(String data) {
        this(new FontSize(data));
    }

    @Override
    public FontSize getDefaultData() {
        return FontSize.DEFAULT;
    }
}
