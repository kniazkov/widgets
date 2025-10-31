/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

public interface HandlesTextEvents extends HandlesEvents {
    default void onTextInput(Controller<String> ctrl) {
        this.setController(Event.TEXT_INPUT, ctrl);
    }
}
