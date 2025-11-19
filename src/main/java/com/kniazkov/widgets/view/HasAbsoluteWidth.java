/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that has an absolute width expressed in fixed units
 * such as pixels, points, or other {@link AbsoluteSize} values.
 */
public interface HasAbsoluteWidth extends Entity {

    /**
     * Returns the model that stores the absolute width for this view.
     *
     * @return the width model
     */
    default Model<AbsoluteSize> getWidthModel() {
        return this.getModel(State.ANY, Property.ABSOLUTE_WIDTH);
    }

    /**
     * Sets a new model that stores the absolute width for this view.
     *
     * @param model the width model to set
     */
    default void setWidthModel(final Model<AbsoluteSize> model) {
        this.setModel(State.ANY, Property.ABSOLUTE_WIDTH, model);
    }

    /**
     * Returns the current absolute width from the current model.
     *
     * @return the current absolute width
     */
    default AbsoluteSize getWidth() {
        return this.getWidthModel().getData();
    }

    /**
     * Updates the absolute width value in the associated model.
     *
     * @param width the new absolute width
     */
    default void setWidth(final AbsoluteSize width) {
        this.getWidthModel().setData(width);
    }

    /**
     * Updates the absolute width value in the associated model by parsing the given string into
     * an {@link AbsoluteSize}.
     *
     * @param width the new absolute width string to parse and set
     */
    default void setWidth(final String width) {
        this.getWidthModel().setData(AbsoluteSize.parse(width));
    }
}
