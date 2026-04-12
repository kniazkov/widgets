/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Marker base class for block widgets. A {@code BlockWidget} represents a block UI element that
 * typically occupies the full width of their parent container and are visually separated from
 * surrounding inline content.
 *
 * @param <S> Widget style
 */
public abstract class BlockWidget<S extends Style> extends Widget<S> {
    /**
     * Creates a new block widget instance initialized from the specified {@link Style}.
     *
     * @param style the style providing the initial models and properties for this widget
     */
    public BlockWidget(final S style) {
        super(style);
    }
}
