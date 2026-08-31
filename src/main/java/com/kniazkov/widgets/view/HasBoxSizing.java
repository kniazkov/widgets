/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.BoxSizing;
import com.kniazkov.widgets.model.Model;

/**
 * An entity that controls how borders and padding participate in its size.
 */
public interface HasBoxSizing extends Entity {
    /**
     * Returns the box-sizing model.
     *
     * @return box-sizing model
     */
    default Model<BoxSizing> getBoxSizingModel() {
        return this.getModel(State.ANY, Property.BOX_SIZING);
    }

    /**
     * Sets the box-sizing model.
     *
     * @param model box-sizing model
     */
    default void setBoxSizingModel(final Model<BoxSizing> model) {
        this.setModel(State.ANY, Property.BOX_SIZING, model);
    }

    /**
     * Returns the box-sizing value.
     *
     * @return box-sizing value
     */
    default BoxSizing getBoxSizing() {
        return this.getBoxSizingModel().getData();
    }

    /**
     * Sets the box-sizing value.
     *
     * @param boxSizing box-sizing value
     */
    default void setBoxSizing(final BoxSizing boxSizing) {
        this.getBoxSizingModel().setData(boxSizing);
    }
}
