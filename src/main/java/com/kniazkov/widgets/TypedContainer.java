/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * An entity that contains other entities of a specific type.
 * @param <T> Type of child entities
 */
public interface TypedContainer<T extends Widget> extends Container {
    /**
     * Returns the child entity by its index.
     * @param index Index
     * @return Child entity
     */
    @Override
    @NotNull T getChild(final int index) throws IndexOutOfBoundsException;
}
