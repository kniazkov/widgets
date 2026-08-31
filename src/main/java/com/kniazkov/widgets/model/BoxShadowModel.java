/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.BoxShadow;

/**
 * Default model for a box shadow.
 */
public final class BoxShadowModel extends DefaultModel<BoxShadow> {
    /**
     * Creates a model without a shadow.
     */
    public BoxShadowModel() {
    }

    /**
     * Creates a model with a shadow.
     *
     * @param data initial shadow
     */
    public BoxShadowModel(final BoxShadow data) {
        super(data);
    }

    @Override
    protected BoxShadow getDefaultData() {
        return BoxShadow.NONE;
    }

    @Override
    public Model<BoxShadow> deriveWithData(final BoxShadow data) {
        return new BoxShadowModel(data);
    }
}
