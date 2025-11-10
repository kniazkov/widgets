/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Offset;

/**
 * A default offset model implementation.
 */
public final class OffsetModel extends DefaultModel<Offset> {
    /**
     * Creates a new offset model initialized with
     * {@link Offset#UNDEFINED}.
     */
    public OffsetModel() {
    }

    /**
     * Creates a new offset model initialized with the specified value.
     *
     * @param data the initial offset
     */
    public OffsetModel(final Offset data) {
        super(data);
    }

    @Override
    public Offset getDefaultData() {
        return Offset.UNDEFINED;
    }

    @Override
    public Model<Offset> deriveWithData(final Offset data) {
        return new OffsetModel(data);
    }
}
