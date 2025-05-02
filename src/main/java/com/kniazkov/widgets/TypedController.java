/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A controller that reacts to events carrying additional data of a specific type.
 *
 * @param <T> The type of data associated with the event
 */
public interface TypedController<T> {
    /**
     * Handles an event with the given data.
     * <p>
     *     This method is called when an event occurs and the corresponding value of type {@code T}
     *     is passed along. Used in contexts like input changes, selection updates, etc.
     * </p>
     *
     * @param data Event-specific data
     */
    void handleEvent(T data);
}
