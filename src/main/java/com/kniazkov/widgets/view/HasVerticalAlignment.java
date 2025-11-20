/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.VerticalAlignment;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that has an associated vertical alignment model.
 */
public interface HasVerticalAlignment extends Entity {

    /**
     * Returns the model that stores the vertical alignment for this view.
     *
     * @return the vertical alignment model
     */
    default Model<VerticalAlignment> getVerticalAlignmentModel() {
        return this.getModel(State.ANY, Property.VERTICAL_ALIGNMENT);
    }

    /**
     * Sets a new vertical alignment model for this view.
     *
     * @param model the alignment model to set
     */
    default void setVerticalAlignmentModel(Model<VerticalAlignment> model) {
        this.setModel(State.ANY, Property.VERTICAL_ALIGNMENT, model);
    }

    /**
     * Returns the current vertical alignment from the model.
     *
     * @return the current vertical alignment
     */
    default VerticalAlignment getVerticalAlignment() {
        return this.getVerticalAlignmentModel().getData();
    }

    /**
     * Updates the vertical alignment in the model.
     *
     * @param alignment the new vertical alignment
     */
    default void setVerticalAlignment(VerticalAlignment alignment) {
        this.getVerticalAlignmentModel().setData(alignment);
    }
}
