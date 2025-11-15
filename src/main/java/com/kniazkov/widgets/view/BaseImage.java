/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

public abstract class BaseImage extends InlineWidget implements HasBorder,
        HasAbsoluteWidth, HasAbsoluteHeight
{
    public static ImageStyle getDefaultStyle() {
        return ImageStyle.DEFAULT;
    }

    public BaseImage() {
        super(getDefaultStyle());
    }

    public void setStyle(ImageStyle style) {
        super.setStyle(style);
    }
}
