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
        implements HasBorder, HasMargin, HasAbsoluteWidth, HasAbsoluteHeight, HasOpacity,
        HasBoxShadow, HasCursor, HasTransition, HasBoxSizing {
    /**
     * Creates a new image widget.
     *
     * @param style Widget style
     */
    public BaseImageWidget(final S style) {
        super(style);
    }

    /**
     * Reserves the image box before its bytes arrive using HTML width/height attributes.
     * CSS size limits still apply; the image is contained in the box without distortion.
     * Supply dimensions from trusted image metadata, not the configured maximum limits.
     *
     * @param width original width in pixels, positive
     * @param height original height in pixels, positive
     */
    public void setIntrinsicSize(final int width, final int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }
        this.getModel(State.ANY, Property.INTRINSIC_SIZE).setData(width + " " + height);
    }

    /**
     * Removes the reserved size and lets the browser discover the image dimensions again.
     */
    public void clearIntrinsicSize() {
        this.getModel(State.ANY, Property.INTRINSIC_SIZE).setData("");
    }
}
