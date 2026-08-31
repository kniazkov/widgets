/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.BoxShadow;
import com.kniazkov.widgets.model.Model;

/**
 * An entity with a state-dependent box shadow.
 */
public interface HasBoxShadow extends Entity {
    /**
     * Returns the box-shadow model for a state.
     *
     * @param state widget state
     * @return box-shadow model
     */
    default Model<BoxShadow> getBoxShadowModel(final State state) {
        return this.getModel(state, Property.BOX_SHADOW);
    }

    /**
     * Sets the box-shadow model for a state.
     *
     * @param state widget state
     * @param model box-shadow model
     */
    default void setBoxShadowModel(final State state, final Model<BoxShadow> model) {
        this.setModel(state, Property.BOX_SHADOW, model);
    }

    /**
     * Returns the box shadow for a state.
     *
     * @param state widget state
     * @return box shadow
     */
    default BoxShadow getBoxShadow(final State state) {
        return this.getBoxShadowModel(state).getData();
    }

    /**
     * Returns the normal-state box shadow.
     *
     * @return box shadow
     */
    default BoxShadow getBoxShadow() {
        return this.getBoxShadow(State.NORMAL);
    }

    /**
     * Sets the box shadow for a state.
     *
     * @param state widget state
     * @param shadow box shadow
     */
    default void setBoxShadow(final State state, final BoxShadow shadow) {
        this.getBoxShadowModel(state).setData(shadow);
    }

    /**
     * Sets the same box shadow for every supported state.
     *
     * @param shadow box shadow
     */
    default void setBoxShadow(final BoxShadow shadow) {
        for (final State state : this.getSupportedStates()) {
            this.setBoxShadow(state, shadow);
        }
    }
}
