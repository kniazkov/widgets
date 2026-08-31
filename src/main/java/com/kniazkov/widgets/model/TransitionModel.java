/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Transition;

/**
 * Default model for a transition.
 */
public final class TransitionModel extends DefaultModel<Transition> {
    /**
     * Creates a model without a transition.
     */
    public TransitionModel() {
    }

    /**
     * Creates a model with a transition.
     *
     * @param data initial transition
     */
    public TransitionModel(final Transition data) {
        super(data);
    }

    @Override
    protected Transition getDefaultData() {
        return Transition.NONE;
    }

    @Override
    public Model<Transition> deriveWithData(final Transition data) {
        return new TransitionModel(data);
    }
}
