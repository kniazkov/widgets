/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

/**
 * A controller that reacts to events carrying additional data of a specific type.
 *
 * @param <T> The type of data associated with the event
 */
public interface TypedController<T> {
    /**
     * Handles an event with the given data.
     *
     * @param data Event-specific data
     */
    void handleEvent(T data);
}
