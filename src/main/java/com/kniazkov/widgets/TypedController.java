/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Interface that describes the behavior when some event occurs.
 * This event contains additional data of a specified type.
 * @param <T> Type of event data
 */
public interface TypedController<T> {
    /**
     * Method called when some event occurs.
     * @param data Data that is transmitted with the event
     */
    void handleEvent(@NotNull T data);
}
