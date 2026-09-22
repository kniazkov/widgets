/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * Reactive policy for removing a modal popup when its backdrop is clicked or tapped.
 */
public interface HasCloseOnOutsideClick extends Entity {
    /**
     * Returns the outside-click dismissal model.
     * @return boolean model, false by default
     */
    default Model<Boolean> getCloseOnOutsideClickModel() {
        return this.getModel(State.ANY, Property.CLOSE_ON_OUTSIDE_CLICK);
    }

    /**
     * Replaces the outside-click dismissal model.
     * @param model replacement model
     */
    default void setCloseOnOutsideClickModel(final Model<Boolean> model) {
        this.setModel(State.ANY, Property.CLOSE_ON_OUTSIDE_CLICK, model);
    }

    /**
     * Returns whether clicking or tapping the backdrop removes the popup.
     * @return whether outside-click dismissal is enabled
     */
    default boolean isCloseOnOutsideClick() {
        return this.getCloseOnOutsideClickModel().getData();
    }

    /**
     * Enables or disables backdrop dismissal for an existing or future popup.
     * @param enabled true to remove the popup on a backdrop click or tap
     */
    default void setCloseOnOutsideClick(final boolean enabled) {
        this.getCloseOnOutsideClickModel().setData(enabled);
    }
}
