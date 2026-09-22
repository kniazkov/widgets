/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * Reactive policy for fitting and centering content inside a zoom viewport.
 */
public interface HasFitContent extends Entity {
    /**
     * Returns the content-fitting model.
     * @return boolean model, false by default
     */
    default Model<Boolean> getFitContentModel() {
        return this.getModel(State.ANY, Property.FIT_CONTENT);
    }

    /**
     * Replaces the content-fitting model.
     * @param model replacement model
     */
    default void setFitContentModel(final Model<Boolean> model) {
        this.setModel(State.ANY, Property.FIT_CONTENT, model);
    }

    /**
     * Returns whether the initial view fits the entire content.
     * @return whether content-fitting is enabled
     */
    default boolean isFitContent() {
        return this.getFitContentModel().getData();
    }

    /**
     * Enables or disables fitting and centering the initial zoom view.
     * @param enabled true to fit the content to the viewport
     */
    default void setFitContent(final boolean enabled) {
        this.getFitContentModel().setData(enabled);
    }
}
