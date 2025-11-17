/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.HorizontalAlignment;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that has an associated horizontal alignment model.
 */
public interface HasHorizontalAlignment extends Entity {

    /**
     * Returns the model that stores the horizontal alignment for this view.
     *
     * @return the horizontal alignment model
     */
    default Model<HorizontalAlignment> getHorizontalAlignmentModel() {
        return this.getModel(State.ANY, Property.H_ALIGN);
    }

    /**
     * Sets a new horizontal alignment model for this view.
     *
     * @param model the alignment model to set
     */
    default void setHorizontalAlignmentModel(Model<HorizontalAlignment> model) {
        this.setModel(State.ANY, Property.H_ALIGN, model);
    }

    /**
     * Returns the current horizontal alignment from the model.
     *
     * @return the current horizontal alignment
     */
    default HorizontalAlignment getHorizontalAlignment() {
        return this.getHorizontalAlignmentModel().getData();
    }

    /**
     * Updates the horizontal alignment in the model.
     *
     * @param alignment the new horizontal alignment
     */
    default void setHorizontalAlignment(HorizontalAlignment alignment) {
        this.getHorizontalAlignmentModel().setData(alignment);
    }
}
