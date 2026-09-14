/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Cursor;

/**
 * Style definition for {@link Carousel}.
 */
public class CarouselStyle extends ImageWidgetStyle {
    /**
     * The global default carousel style.
     */
    public static final CarouselStyle DEFAULT = new CarouselStyle();

    /**
     * Creates the default carousel style.
     */
    protected CarouselStyle() {
        this.setCursor(Cursor.POINTER);
    }

    /**
     * Creates a new carousel style that inherits models from the specified parent.
     *
     * @param parent parent style
     */
    public CarouselStyle(final CarouselStyle parent) {
        super(parent);
    }

    @Override
    public CarouselStyle derive() {
        return new CarouselStyle(this);
    }
}
