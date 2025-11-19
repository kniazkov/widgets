/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.WidgetSize;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that has a height represented by a generic {@link WidgetSize}.
 * <p>
 * The height may be either an absolute size
 * (for example {@link com.kniazkov.widgets.common.AbsoluteSize})
 * or a relative size such as {@code 50%}, depending on the concrete
 * {@link WidgetSize} implementation stored in the associated model.
 */
public interface HasHeight extends Entity {

    /**
     * Returns the model that stores the height for this view.
     *
     * @return the height model
     */
    default Model<WidgetSize> getHeightModel() {
        return this.getModel(State.ANY, Property.HEIGHT);
    }

    /**
     * Sets a new model that stores the height for this view.
     *
     * @param model the height model to set
     */
    default void setHeightModel(final Model<WidgetSize> model) {
        this.setModel(State.ANY, Property.HEIGHT, model);
    }

    /**
     * Returns the current height value from the associated model.
     *
     * @return the current height
     */
    default WidgetSize getHeight() {
        return this.getHeightModel().getData();
    }

    /**
     * Updates the height value in the associated model.
     *
     * @param height the new height
     */
    default void setHeight(final WidgetSize height) {
        this.getHeightModel().setData(height);
    }

    /**
     * Updates the height value in the associated model by parsing the given string into
     * an {@link WidgetSize}.
     *
     * @param height the new absolute height string to parse and set
     */
    default void setHeight(final String height) {
        this.getHeightModel().setData(WidgetSize.parse(height));
    }
}
