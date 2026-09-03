/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.HandlesSelectionEvents;
import com.kniazkov.widgets.model.Model;

/**
 * An entity that exposes a reactive selected-index model.
 */
public interface HasSelectedIndex extends Entity, HandlesSelectionEvents {
    /**
     * Returns the selected-index model.
     *
     * @return selected-index model
     */
    default Model<Integer> getSelectedIndexModel() {
        return this.getModel(State.ANY, Property.SELECTED_INDEX);
    }

    /**
     * Replaces the selected-index model.
     *
     * @param model replacement model
     */
    default void setSelectedIndexModel(final Model<Integer> model) {
        this.setModel(State.ANY, Property.SELECTED_INDEX, model);
    }

    /**
     * Returns the selected position.
     *
     * @return selected index, or {@code -1} when there is no selection
     */
    default int getSelectedIndex() {
        return this.getSelectedIndexModel().getData();
    }

    /**
     * Selects a position.
     *
     * @param index position to select, or {@code -1} to clear selection
     */
    default void setSelectedIndex(final int index) {
        this.getSelectedIndexModel().setData(index);
    }
}
