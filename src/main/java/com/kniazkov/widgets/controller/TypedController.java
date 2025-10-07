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

    /**
     * A shared stub controller instance that ignores all events.
     * Intended to be used internally by {@link #stub()}.
     */
    TypedController<?> STUB_CONTROLLER = data -> { };

    /**
     * Creates a no-op (stub) controller for any type.
     * This method provides a type-safe way to obtain a default controller
     * that performs no action. Useful as a placeholder to avoid {@code null} checks.
     *
     * @param <T> the event data type
     * @return a stub controller that ignores all events
     */
    @SuppressWarnings("unchecked")
    static <T> TypedController<T> stub() {
        return (TypedController<T>) STUB_CONTROLLER;
    }
}
