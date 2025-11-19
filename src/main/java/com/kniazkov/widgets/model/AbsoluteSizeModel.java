/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.AbsoluteSize;

/**
 * A default absolute size model implementation.
 */
public final class AbsoluteSizeModel extends DefaultModel<AbsoluteSize> {
    /**
     * Creates a new absolute size model initialized with
     * {@link AbsoluteSize#UNDEFINED}.
     */
    public AbsoluteSizeModel() {
    }

    /**
     * Creates a new absolute size model initialized with the specified value.
     *
     * @param data the initial absolute size
     */
    public AbsoluteSizeModel(final AbsoluteSize data) {
        super(data);
    }

    /**
     * Creates a new absolute size model initialized from a CSS-style string.
     * The string is parsed according to {@link AbsoluteSize#parse(String)} (String)},
     * supporting units like {@code px}, {@code pt}, {@code in}, etc.
     *
     * @param data the size string (e.g., "10pt", "24px", "1in")
     * @throws IllegalArgumentException if the string is invalid
     */
    public AbsoluteSizeModel(final String data) {
        this(AbsoluteSize.parse(data));
    }

    @Override
    public AbsoluteSize getDefaultData() {
        return AbsoluteSize.UNDEFINED;
    }

    @Override
    public Model<AbsoluteSize> deriveWithData(final AbsoluteSize data) {
        return new AbsoluteSizeModel(data);
    }
}
