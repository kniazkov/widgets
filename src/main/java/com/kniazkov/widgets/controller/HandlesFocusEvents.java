/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

/**
 * An entity capable of handling focus transitions.
 */
public interface HandlesFocusEvents extends HandlesEvents {
    /**
     * Registers a controller invoked when the element receives focus.
     *
     * @param ctrl controller to execute on focus
     */
    default void onFocus(final Controller<Void> ctrl) {
        this.setController(Event.FOCUS, ctrl);
        this.subscribeToEvent(Event.FOCUS);
    }

    /**
     * Registers a controller invoked when the element loses focus.
     *
     * @param ctrl controller to execute on blur
     */
    default void onBlur(final Controller<Void> ctrl) {
        this.setController(Event.BLUR, ctrl);
        this.subscribeToEvent(Event.BLUR);
    }
}
