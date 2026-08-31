/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Outline;
import com.kniazkov.widgets.model.Model;

/**
 * An entity with a state-dependent outline.
 */
public interface HasOutline extends Entity {
    /**
     * Returns the outline model for a state.
     *
     * @param state widget state
     * @return outline model
     */
    default Model<Outline> getOutlineModel(final State state) {
        return this.getModel(state, Property.OUTLINE);
    }

    /**
     * Sets the outline model for a state.
     *
     * @param state widget state
     * @param model outline model
     */
    default void setOutlineModel(final State state, final Model<Outline> model) {
        this.setModel(state, Property.OUTLINE, model);
    }

    /**
     * Returns the outline for a state.
     *
     * @param state widget state
     * @return outline
     */
    default Outline getOutline(final State state) {
        return this.getOutlineModel(state).getData();
    }

    /**
     * Returns the normal-state outline.
     *
     * @return outline
     */
    default Outline getOutline() {
        return this.getOutline(State.NORMAL);
    }

    /**
     * Sets the outline for a state.
     *
     * @param state widget state
     * @param outline outline
     */
    default void setOutline(final State state, final Outline outline) {
        this.getOutlineModel(state).setData(outline);
    }

    /**
     * Sets the same outline for every supported state.
     *
     * @param outline outline
     */
    default void setOutline(final Outline outline) {
        for (final State state : this.getSupportedStates()) {
            this.setOutline(state, outline);
        }
    }
}
