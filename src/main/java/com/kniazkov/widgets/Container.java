/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * An entity that contains other entities of a specific type.
 * @param <T> Type of child entities
 */
public interface Container<T> {
    /**
     * Returns the number of child entities.
     * @return Number of child entities
     */
    int getChildCount();

    /**
     * Returns the child entity by its index.
     * @param index Index
     * @return Child entity
     */
    @NotNull T getChild(final int index) throws IndexOutOfBoundsException;
}
