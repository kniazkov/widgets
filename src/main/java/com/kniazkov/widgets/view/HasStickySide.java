/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.StickySide;
import com.kniazkov.widgets.model.Model;

/**
 * An entity whose sticky viewport edge is controlled by a reactive model.
 */
public interface HasStickySide extends Entity {
    /**
     * Returns the model that selects the sticky edge.
     *
     * @return sticky side model
     */
    default Model<StickySide> getStickySideModel() {
        return this.getModel(State.ANY, Property.STICKY_SIDE);
    }

    /**
     * Replaces the model that selects the sticky edge.
     *
     * @param model new sticky side model
     */
    default void setStickySideModel(final Model<StickySide> model) {
        this.setModel(State.ANY, Property.STICKY_SIDE, model);
    }

    /**
     * Returns the currently selected sticky edge.
     *
     * @return sticky edge
     */
    default StickySide getStickySide() {
        return this.getStickySideModel().getData();
    }

    /**
     * Selects the viewport edge to which the widget sticks.
     *
     * @param side sticky edge
     */
    default void setStickySide(final StickySide side) {
        this.getStickySideModel().setData(side);
    }

    /**
     * Configures the widget to stick to the top edge.
     */
    default void stickToTop() {
        this.setStickySide(StickySide.TOP);
    }

    /**
     * Configures the widget to stick to the bottom edge.
     */
    default void stickToBottom() {
        this.setStickySide(StickySide.BOTTOM);
    }
}
