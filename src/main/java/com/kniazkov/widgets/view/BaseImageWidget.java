/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Base class for all image widgets.
 *
 * @param <S> Widget style
 */
public abstract class BaseImageWidget<S extends Style> extends InlineWidget<S>
        implements HasBorder, HasMargin, HasAbsoluteWidth, HasAbsoluteHeight, HasOpacity {
    /**
     * Creates a new image widget.
     *
     * @param style Widget style
     */
    public BaseImageWidget(final S style) {
        super(style);
    }
}
