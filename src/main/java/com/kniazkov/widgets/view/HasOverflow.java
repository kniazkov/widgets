/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Overflow;
import com.kniazkov.widgets.model.Model;

/**
 * An entity that controls how content outside its box is displayed.
 */
public interface HasOverflow extends Entity {
    /**
     * Returns the overflow model.
     *
     * @return overflow model
     */
    default Model<Overflow> getOverflowModel() {
        return this.getModel(State.ANY, Property.OVERFLOW);
    }

    /**
     * Sets the overflow model.
     *
     * @param model overflow model
     */
    default void setOverflowModel(final Model<Overflow> model) {
        this.setModel(State.ANY, Property.OVERFLOW, model);
    }

    /**
     * Returns the overflow value.
     *
     * @return overflow value
     */
    default Overflow getOverflow() {
        return this.getOverflowModel().getData();
    }

    /**
     * Sets the overflow value.
     *
     * @param overflow overflow value
     */
    default void setOverflow(final Overflow overflow) {
        this.getOverflowModel().setData(overflow);
    }
}
