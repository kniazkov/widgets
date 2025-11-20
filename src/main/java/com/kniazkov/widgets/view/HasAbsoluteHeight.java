/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that has an absolute height expressed in fixed units
 * such as pixels, points, or other {@link AbsoluteSize} values.
 */
public interface HasAbsoluteHeight extends Entity {
    /**
     * Returns the model that stores the absolute height for this view.
     *
     * @return the height model
     */
    default Model<AbsoluteSize> getHeightModel() {
        return this.getModel(State.ANY, Property.ABSOLUTE_HEIGHT);
    }

    /**
     * Sets a new model that stores the absolute height for this view.
     *
     * @param model the height model to set
     */
    default void setHeightModel(final Model<AbsoluteSize> model) {
        this.setModel(State.ANY, Property.ABSOLUTE_HEIGHT, model);
    }

    /**
     * Returns the current absolute height from the current model.
     *
     * @return the current absolute height
     */
    default AbsoluteSize getHeight() {
        return this.getHeightModel().getData();
    }

    /**
     * Updates the absolute height value in the associated model.
     *
     * @param height the new absolute height
     */
    default void setHeight(final AbsoluteSize height) {
        this.getHeightModel().setData(height);
    }

    /**
     * Updates the absolute height value in the associated model by parsing the given string into
     * an {@link AbsoluteSize}.
     *
     * @param height the new absolute height string to parse and set
     */
    default void setHeight(final String height) {
        this.getHeightModel().setData(AbsoluteSize.parse(height));
    }

    /**
     * Updates the absolute height value in the associated model using a pixel value.
     *
     * @param px the new height in pixels (must be ≥ 0)
     */
    default void setHeight(final int px) {
        this.getHeightModel().setData(new AbsoluteSize(px));
    }
}
