/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.images.SvgImageSource;

/**
 * A model that stores an {@link SvgImageSource} value.
 */
public final class SvgImageSourceModel extends DefaultModel<SvgImageSource> {
    /**
     * Creates a new SVG image source model initialized with the default value.
     */
    public SvgImageSourceModel() {
    }

    /**
     * Creates a new SVG image source model initialized with the specified value.
     *
     * @param data the initial {@code SvgImageSource} value
     */
    public SvgImageSourceModel(final SvgImageSource data) {
        super(data);
    }

    /**
     * Returns the default SVG image source value.
     *
     * @return an empty valid SVG image source
     */
    @Override
    public SvgImageSource getDefaultData() {
        return SvgImageSource.EMPTY;
    }

    /**
     * Creates a derived model instance with the specified data.
     *
     * @param data the data for the derived model
     * @return a new model instance containing the specified data
     */
    @Override
    public Model<SvgImageSource> deriveWithData(final SvgImageSource data) {
        return new SvgImageSourceModel(data);
    }
}
