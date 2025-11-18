/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Base class for all image widgets.
 * <p>
 * This class defines that every image uses an {@link ImageStyle} and provides the default style
 * applied at construction time. Subclasses inherit the styling mechanism and can override
 * or adjust the assigned style as needed.
 */
public abstract class BaseImage extends InlineWidget implements HasBorder,
        HasAbsoluteWidth, HasAbsoluteHeight {

    /**
     * Returns the default style applied to all images.
     *
     * @return the default {@link ImageStyle}
     */
    public static ImageStyle getDefaultStyle() {
        return ImageStyle.DEFAULT;
    }

    /**
     * Creates a new image widget initialized with the default style.
     */
    public BaseImage() {
        super(getDefaultStyle());
    }

    /**
     * Sets the style used to render this image.
     *
     * @param style the {@link ImageStyle} to apply
     */
    public void setStyle(final ImageStyle style) {
        super.setStyle(style);
    }
}
