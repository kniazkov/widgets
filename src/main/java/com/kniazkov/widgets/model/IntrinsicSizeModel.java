/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.IntrinsicSize;
import java.util.Objects;

/**
 * Observable image dimensions with an explicit no-value default.
 */
public final class IntrinsicSizeModel extends DefaultModel<IntrinsicSize> {
    /**
     * Creates a model with no dimensions supplied.
     */
    public IntrinsicSizeModel() {
    }

    /**
     * @param data initial dimensions, including IntrinsicSize.NONE
     */
    public IntrinsicSizeModel(final IntrinsicSize data) {
        super(Objects.requireNonNull(data, "data"));
    }

    @Override
    public IntrinsicSize getDefaultData() {
        return IntrinsicSize.NONE;
    }

    @Override
    public boolean setData(final IntrinsicSize data) {
        return super.setData(Objects.requireNonNull(data, "data"));
    }

    @Override
    public Model<IntrinsicSize> deriveWithData(final IntrinsicSize data) {
        return new IntrinsicSizeModel(data);
    }
}
