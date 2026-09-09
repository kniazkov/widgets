/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;

/**
 * Style definition for a modal popup and its backdrop.
 */
public class ModalPopupStyle extends PopupStyle implements HasBackdropColor {
    /**
     * Global default modal popup style.
     */
    public static final ModalPopupStyle DEFAULT = new ModalPopupStyle();

    /**
     * Creates the default modal popup style.
     */
    private ModalPopupStyle() {
        this.setBackdropColor(new Color(255, 255, 255, 192));
    }

    /**
     * Creates a derived modal popup style.
     *
     * @param parent parent style
     */
    public ModalPopupStyle(final ModalPopupStyle parent) {
        super(parent);
    }

    @Override
    public ModalPopupStyle derive() {
        return new ModalPopupStyle(this);
    }
}
