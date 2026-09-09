/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * A popup window with a backdrop that blocks interaction with the underlying page.
 */
public class ModalPopup extends Popup implements HasBackdropColor {
    /**
     * Returns the global default modal popup style.
     *
     * @return default modal popup style
     */
    public static ModalPopupStyle getDefaultStyle() {
        return ModalPopupStyle.DEFAULT;
    }

    /**
     * Creates an empty modal popup with the default style.
     */
    public ModalPopup() {
        super(getDefaultStyle());
    }

    /**
     * Creates a modal popup containing the specified block widgets.
     *
     * @param children initial children
     */
    public ModalPopup(final BlockWidget<?>... children) {
        super(getDefaultStyle(), children);
    }

    /**
     * Creates an empty modal popup with the specified style.
     *
     * @param style popup style
     */
    public ModalPopup(final ModalPopupStyle style) {
        super(style);
    }

    /**
     * Creates a modal popup with the specified style and children.
     *
     * @param style popup style
     * @param children initial children
     */
    public ModalPopup(final ModalPopupStyle style, final BlockWidget<?>... children) {
        super(style, children);
    }

    @Override
    public String getType() {
        return "modal popup";
    }
}
