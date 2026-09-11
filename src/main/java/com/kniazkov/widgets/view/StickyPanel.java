/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * A block container that remains in normal document flow until scrolling reaches a configured
 * viewport edge. It then stays at that edge while its containing block remains visible.
 * Unlike a popup, a sticky panel keeps its original place in page layout.
 */
public final class StickyPanel extends Panel implements HasStickySide {
    /**
     * Returns the global default sticky panel style.
     *
     * @return default style
     */
    public static StickyPanelStyle getDefaultStyle() {
        return StickyPanelStyle.DEFAULT;
    }

    /**
     * Creates an empty panel that sticks to the top edge by default.
     */
    public StickyPanel() {
        super(getDefaultStyle());
    }

    /**
     * Creates a sticky panel containing the specified block widgets.
     *
     * @param children initial child widgets, in display order
     */
    public StickyPanel(final BlockWidget<?>... children) {
        super(getDefaultStyle(), children);
    }

    /**
     * Creates an empty sticky panel with the specified style.
     *
     * @param style sticky panel style
     */
    public StickyPanel(final StickyPanelStyle style) {
        super(style);
    }

    /**
     * Creates a sticky panel with the specified style and block widgets.
     *
     * @param style sticky panel style
     * @param children initial child widgets, in display order
     */
    public StickyPanel(final StickyPanelStyle style, final BlockWidget<?>... children) {
        super(style, children);
    }

    @Override
    public String getType() {
        return "sticky panel";
    }
}
