/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.WidgetSize;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that has a maximum width represented by a generic {@link WidgetSize}.
 */
public interface HasMaxWidth extends Entity {

    /**
     * Returns the model that stores the maximum width for this view.
     *
     * @return the maximum width model
     */
    default Model<WidgetSize> getMaxWidthModel() {
        return this.getModel(State.ANY, Property.MAX_WIDTH);
    }

    /**
     * Sets a new model that stores the maximum width for this view.
     *
     * @param model the maximum width model to set
     */
    default void setMaxWidthModel(final Model<WidgetSize> model) {
        this.setModel(State.ANY, Property.MAX_WIDTH, model);
    }

    /**
     * Returns the current maximum width from the associated model.
     *
     * @return the current maximum width
     */
    default WidgetSize getMaxWidth() {
        return this.getMaxWidthModel().getData();
    }

    /**
     * Updates the maximum width value in the associated model.
     *
     * @param width the new maximum width
     */
    default void setMaxWidth(final WidgetSize width) {
        this.getMaxWidthModel().setData(width);
    }

    /**
     * Updates the maximum width value in the associated model by parsing the given string into
     * an {@link WidgetSize}.
     *
     * @param width the new maximum width string to parse and set
     */
    default void setMaxWidth(final String width) {
        this.getMaxWidthModel().setData(WidgetSize.parse(width));
    }

    /**
     * Updates the maximum width value in the associated model using a pixel value.
     *
     * @param px the new maximum width in pixels (must be ≥ 0)
     */
    default void setMaxWidth(final int px) {
        this.getMaxWidthModel().setData(new AbsoluteSize(px));
    }
}

