/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * A model listener that is called when model data has been changed.
 * @param <T> Type of model data
 */
public interface ModelListener<T> {
    /**
     * Method that is called when the data has been changed.
     * @param data New data
     */
    void dataChanged(@NotNull T data);
}
