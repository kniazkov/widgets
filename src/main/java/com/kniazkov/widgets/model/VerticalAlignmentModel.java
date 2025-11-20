/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.VerticalAlignment;

/**
 * A model that stores a {@link VerticalAlignment} value.
 */
public final class VerticalAlignmentModel extends DefaultModel<VerticalAlignment> {

    /**
     * Creates a new model initialized with the default alignment.
     */
    public VerticalAlignmentModel() {
    }

    /**
     * Creates a new model initialized with the specified alignment.
     *
     * @param data the initial alignment value
     */
    public VerticalAlignmentModel(final VerticalAlignment data) {
        super(data);
    }

    @Override
    public VerticalAlignment getDefaultData() {
        return VerticalAlignment.TOP;
    }

    @Override
    public Model<VerticalAlignment> deriveWithData(final VerticalAlignment data) {
        return new VerticalAlignmentModel(data);
    }
}
