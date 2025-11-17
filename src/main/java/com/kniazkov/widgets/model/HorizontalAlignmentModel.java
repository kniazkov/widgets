/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.HorizontalAlignment;

/**
 * A model that stores a {@link HorizontalAlignment} value.
 */
public final class HorizontalAlignmentModel extends DefaultModel<HorizontalAlignment> {

    /**
     * Creates a new model initialized with the default alignment.
     */
    public HorizontalAlignmentModel() {
    }

    /**
     * Creates a new model initialized with the specified alignment.
     *
     * @param data the initial alignment value
     */
    public HorizontalAlignmentModel(final HorizontalAlignment data) {
        super(data);
    }

    @Override
    public HorizontalAlignment getDefaultData() {
        return HorizontalAlignment.LEFT;
    }

    @Override
    public Model<HorizontalAlignment> deriveWithData(final HorizontalAlignment data) {
        return new HorizontalAlignmentModel(data);
    }
}
