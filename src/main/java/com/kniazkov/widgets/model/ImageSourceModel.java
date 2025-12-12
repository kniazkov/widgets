/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.images.ImageSource;

/**
 * A model that stores an {@link ImageSource} value.
 */
public final class ImageSourceModel extends DefaultModel<ImageSource> {
    /**
     * Creates a new image source model initialized with the default value.
     */
    public ImageSourceModel() {
    }

    /**
     * Creates a new image source model initialized with the specified value.
     *
     * @param data the initial {@code ImageSource} value
     */
    public ImageSourceModel(final ImageSource data) {
        super(data);
    }

    @Override
    public ImageSource getDefaultData() {
        return ImageSource.INVALID;
    }

    @Override
    public Model<ImageSource> deriveWithData(final ImageSource data) {
        return new ImageSourceModel(data);
    }
}
