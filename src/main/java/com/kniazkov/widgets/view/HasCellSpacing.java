/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.common.AbsoluteSize;

/**
 * An {@link Entity} that has an associated cell spacing model.
 */
public interface HasCellSpacing extends Entity {

    /**
     * Returns the model that stores the cell spacing for this view.
     *
     * @return the cell spacing model
     */
    default Model<AbsoluteSize> getCellSpacingModel() {
        return this.getModel(State.ANY, Property.CELL_SPACING);
    }

    /**
     * Sets a new model that stores the cell spacing for this view.
     *
     * @param model the cell spacing model to set
     */
    default void setCellSpacingModel(final Model<AbsoluteSize> model) {
        this.setModel(State.ANY, Property.CELL_SPACING, model);
    }

    /**
     * Returns the current cell spacing from the current model.
     *
     * @return the current cell spacing
     */
    default AbsoluteSize getCellSpacing() {
        return this.getCellSpacingModel().getData();
    }

    /**
     * Updates the cell spacing value in the associated model.
     *
     * @param spacing the new cell spacing
     */
    default void setCellSpacing(final AbsoluteSize spacing) {
        this.getCellSpacingModel().setData(spacing);
    }

    /**
     * Updates the cell spacing value in the associated model by parsing the given string.
     *
     * @param spacing the new cell spacing string to parse and set
     */
    default void setCellSpacing(final String spacing) {
        this.getCellSpacingModel().setData(AbsoluteSize.parse(spacing));
    }

    /**
     * Updates the cell spacing value in the associated model using a pixel value.
     *
     * @param px the new cell spacing in pixels (must be ≥ 0)
     */
    default void setCellSpacing(final int px) {
        this.getCellSpacingModel().setData(new AbsoluteSize(px));
    }
}
