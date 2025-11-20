/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.WidgetSize;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that has a width represented by a generic {@link WidgetSize}.
 */
public interface HasWidth extends Entity {

    /**
     * Returns the model that stores the width for this view.
     *
     * @return the width model
     */
    default Model<WidgetSize> getWidthModel() {
        return this.getModel(State.ANY, Property.WIDTH);
    }

    /**
     * Sets a new model that stores the width for this view.
     *
     * @param model the width model to set
     */
    default void setWidthModel(final Model<WidgetSize> model) {
        this.setModel(State.ANY, Property.WIDTH, model);
    }

    /**
     * Returns the current width from the associated model.
     *
     * @return the current width
     */
    default WidgetSize getWidth() {
        return this.getWidthModel().getData();
    }

    /**
     * Updates the width value in the associated model.
     *
     * @param width the new width
     */
    default void setWidth(final WidgetSize width) {
        this.getWidthModel().setData(width);
    }

    /**
     * Updates the width value in the associated model by parsing the given string into
     * an {@link WidgetSize}.
     *
     * @param width the new width string to parse and set
     */
    default void setWidth(final String width) {
        this.getWidthModel().setData(WidgetSize.parse(width));
    }

    /**
     * Updates the width value in the associated model using a pixel value.
     *
     * @param px the new width in pixels (must be ≥ 0)
     */
    default void setWidth(final int px) {
        this.getWidthModel().setData(new AbsoluteSize(px));
    }
}
