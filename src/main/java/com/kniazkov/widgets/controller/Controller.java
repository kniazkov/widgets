/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

/**
 * Interface that describes the behavior when some event occurs.
 */
public interface Controller {
    /**
     * Method called when some event occurs.
     */
    void handleEvent();

    /**
     * Stub controller that does nothing.
     */
    static Controller STUB_CONTROLLER = () -> { };

    /**
     * Returns stub controller that does nothing.
     */
    static Controller stub() {
        return STUB_CONTROLLER;
    }
}
