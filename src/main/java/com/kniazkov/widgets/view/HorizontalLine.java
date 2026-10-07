/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * A horizontal separator rendered as a semantic HTML hr element.
 * Height controls line thickness; color, width, margins, opacity and visibility are reactive.
 */
public class HorizontalLine extends BlockWidget<HorizontalLineStyle>
        implements HasColor, HasAbsoluteHeight, HasWidth, HasMargin, HasOpacity, HasHiddenState {
    /**
     * Returns the shared default separator style.
     *
     * @return default style
     */
    public static HorizontalLineStyle getDefaultStyle() {
        return HorizontalLineStyle.DEFAULT;
    }

    /**
     * Creates a one-pixel, full-width separator.
     */
    public HorizontalLine() {
        this(getDefaultStyle());
    }

    /**
     * Creates a separator using the supplied style.
     *
     * @param style separator style
     */
    public HorizontalLine(final HorizontalLineStyle style) {
        super(style);
    }

    @Override
    public String getType() {
        return "horizontal line";
    }
}
