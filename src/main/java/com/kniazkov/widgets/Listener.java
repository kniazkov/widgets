/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

package com.kniazkov.widgets;

/**
 * Listener interface for receiving notifications when data changes.
 *  Classes that are interested in processing data change events
 *  should implement this interface and register themselves where necessary.
 * @param <T> The type of the data being observed
 */
public interface Listener<T> {
    /**
     * Invoked when the observed data has changed.
     * @param data the updated data
     */
    void dataChanged(T data);
}
