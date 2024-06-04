/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

/**
 * A listener that is called when data has been changed.
 * @param <T> Type of data
 */
public interface Listener<T> {
    /**
     * Method that is called when the data has been changed.
     * @param data New data
     */
    void dataChanged(@NotNull T data);
}
