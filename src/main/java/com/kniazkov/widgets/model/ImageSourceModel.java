/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.ImageSource;

public final class ImageSourceModel extends DefaultModel<ImageSource> {
    public ImageSourceModel() {
    }

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
