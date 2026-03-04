/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Base class for all image widgets.
 */
public abstract class BaseImageWidget extends InlineWidget implements HasBorder, HasMargin,
        HasAbsoluteWidth, HasAbsoluteHeight, HasOpacity {
    /**
     * Creates a new image widget.
     *
     * @param style Widget style
     */
    public BaseImageWidget(final ImageWidgetStyle style) {
        super(style);
    }
}
