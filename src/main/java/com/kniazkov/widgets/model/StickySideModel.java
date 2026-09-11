/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.StickySide;

/**
 * A model containing the viewport edge used by a sticky widget.
 */
public final class StickySideModel extends DefaultModel<StickySide> {
    /**
     * Creates a model whose default side is {@link StickySide#TOP}.
     */
    public StickySideModel() {
    }

    /**
     * Creates a model initialized with the specified side.
     *
     * @param data initial side
     */
    public StickySideModel(final StickySide data) {
        super(data);
    }

    @Override
    public StickySide getDefaultData() {
        return StickySide.TOP;
    }

    @Override
    public Model<StickySide> deriveWithData(final StickySide data) {
        return new StickySideModel(data);
    }
}
