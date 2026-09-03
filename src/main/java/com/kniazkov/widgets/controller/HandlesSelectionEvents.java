/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

/**
 * An entity capable of reacting when the user selects a position.
 */
public interface HandlesSelectionEvents extends HandlesEvents {
    /**
     * Registers a controller invoked after the selection model has been updated.
     *
     * @param ctrl selection controller
     */
    default void onSelect(final Controller<Integer> ctrl) {
        this.setController(Event.SELECT, ctrl);
        this.subscribeToEvent(Event.SELECT);
    }
}
