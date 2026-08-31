/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.BoxSizing;

/**
 * Default model for box sizing.
 */
public final class BoxSizingModel extends DefaultModel<BoxSizing> {
    /**
     * Creates a content-box model.
     */
    public BoxSizingModel() {
    }

    /**
     * Creates a model with a box-sizing value.
     *
     * @param data initial value
     */
    public BoxSizingModel(final BoxSizing data) {
        super(data);
    }

    @Override
    protected BoxSizing getDefaultData() {
        return BoxSizing.CONTENT_BOX;
    }

    @Override
    public Model<BoxSizing> deriveWithData(final BoxSizing data) {
        return new BoxSizingModel(data);
    }
}
