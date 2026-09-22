/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Style definition for {@link ZoomDecorator}, including its reactive gesture settings.
 */
public class ZoomDecoratorStyle extends InlineBlockStyle implements HasMaxScale, HasFitContent {
    /**
     * Global default style with maxScale set to 8.
     */
    public static final ZoomDecoratorStyle DEFAULT = new ZoomDecoratorStyle();

    /**
     * Creates the default style with the base widget's layout and appearance.
     */
    private ZoomDecoratorStyle() {
        super(InlineBlock.getDefaultStyle());
        this.setMaxScale(8);
        this.setFitContent(false);
    }

    /**
     * Creates a style whose models inherit reactively from the specified parent.
     * @param parent parent style
     */
    public ZoomDecoratorStyle(final ZoomDecoratorStyle parent) {
        super(parent);
    }

    @Override
    public ZoomDecoratorStyle derive() {
        return new ZoomDecoratorStyle(this);
    }
}
