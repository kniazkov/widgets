/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Outline;

/**
 * Default model for an outline.
 */
public final class OutlineModel extends DefaultModel<Outline> {
    /**
     * Creates a model without an outline.
     */
    public OutlineModel() {
    }

    /**
     * Creates a model with an outline.
     *
     * @param data initial outline
     */
    public OutlineModel(final Outline data) {
        super(data);
    }

    @Override
    protected Outline getDefaultData() {
        return Outline.NONE;
    }

    @Override
    public Model<Outline> deriveWithData(final Outline data) {
        return new OutlineModel(data);
    }
}
