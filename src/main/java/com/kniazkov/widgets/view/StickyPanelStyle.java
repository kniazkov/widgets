/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.StickySide;

/**
 * Style definition for {@link StickyPanel}, including its reactive sticky edge.
 */
public final class StickyPanelStyle extends PanelStyle implements HasStickySide {
    /**
     * The global default sticky panel style.
     */
    public static final StickyPanelStyle DEFAULT = new StickyPanelStyle();

    /**
     * Creates the default style, which sticks to the top edge.
     */
    private StickyPanelStyle() {
        super(PanelStyle.DEFAULT);
        this.setStickySide(StickySide.TOP);
    }

    /**
     * Creates a style that inherits all models from the specified parent.
     *
     * @param parent parent sticky panel style
     */
    public StickyPanelStyle(final StickyPanelStyle parent) {
        super(parent);
    }

    @Override
    public StickyPanelStyle derive() {
        return new StickyPanelStyle(this);
    }
}
