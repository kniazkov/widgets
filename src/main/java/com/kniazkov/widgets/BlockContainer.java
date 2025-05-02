/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A container that holds only {@link BlockWidget} instances.
 * <p>
 *     Intended for vertical layout, where widgets are placed one after another,
 *     typically from top to bottom. Each child widget occupies full horizontal width.
 * </p>
 *
 * <p>
 *     This container exposes factory methods for block-level widgets such as {@link Paragraph}.
 * </p>
 */
public interface BlockContainer extends TypedContainer<BlockWidget> {

    /**
     * Creates and adds a new {@link Paragraph} to the container.
     *
     * @return The created {@link Paragraph}
     */
    Paragraph createParagraph();
}