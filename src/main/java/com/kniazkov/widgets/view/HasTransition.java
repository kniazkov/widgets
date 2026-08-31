/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Transition;
import com.kniazkov.widgets.model.Model;

/**
 * An entity with a transition between visual values.
 */
public interface HasTransition extends Entity {
    /**
     * Returns the transition model.
     *
     * @return transition model
     */
    default Model<Transition> getTransitionModel() {
        return this.getModel(State.ANY, Property.TRANSITION);
    }

    /**
     * Sets the transition model.
     *
     * @param model transition model
     */
    default void setTransitionModel(final Model<Transition> model) {
        this.setModel(State.ANY, Property.TRANSITION, model);
    }

    /**
     * Returns the transition.
     *
     * @return transition
     */
    default Transition getTransition() {
        return this.getTransitionModel().getData();
    }

    /**
     * Sets the transition.
     *
     * @param transition transition
     */
    default void setTransition(final Transition transition) {
        this.getTransitionModel().setData(transition);
    }
}
