/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.WidgetSize;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that has a maximum height represented by a generic {@link WidgetSize}.
 */
public interface HasMaxHeight extends Entity {

    /**
     * Returns the model that stores the maximum height for this view.
     *
     * @return the maximum height model
     */
    default Model<WidgetSize> getMaxHeightModel() {
        return this.getModel(State.ANY, Property.MAX_HEIGHT);
    }

    /**
     * Sets a new model that stores the maximum height for this view.
     *
     * @param model the maximum height model to set
     */
    default void setMaxHeightModel(final Model<WidgetSize> model) {
        this.setModel(State.ANY, Property.MAX_HEIGHT, model);
    }

    /**
     * Returns the current maximum height from the associated model.
     *
     * @return the current maximum height
     */
    default WidgetSize getMaxHeight() {
        return this.getMaxHeightModel().getData();
    }

    /**
     * Updates the maximum height value in the associated model.
     *
     * @param height the new maximum height
     */
    default void setMaxHeight(final WidgetSize height) {
        this.getMaxHeightModel().setData(height);
    }

    /**
     * Updates the maximum height value in the associated model by parsing the given string into
     * an {@link WidgetSize}.
     *
     * @param height the new maximum height string to parse and set
     */
    default void setMaxHeight(final String height) {
        this.getMaxHeightModel().setData(WidgetSize.parse(height));
    }

    /**
     * Updates the maximum height value in the associated model using a pixel value.
     *
     * @param px the new maximum height in pixels (must be ≥ 0)
     */
    default void setMaxHeight(final int px) {
        this.getMaxHeightModel().setData(new AbsoluteSize(px));
    }
}

