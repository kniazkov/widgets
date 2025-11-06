/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

/**
 * Extends {@link HandlesEvents} to provide convenient registration of controllers
 * for text-related events such as user input.
 */
public interface HandlesTextEvents extends HandlesEvents {
    /**
     * Registers a controller that will be invoked when a text input event occurs.
     *
     * @param ctrl the controller that handles text input events
     */
    default void onTextInput(Controller<String> ctrl) {
        this.setController(Event.TEXT_INPUT, ctrl);
    }
}
