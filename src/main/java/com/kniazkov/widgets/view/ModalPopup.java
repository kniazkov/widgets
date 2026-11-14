/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * A popup window with a backdrop that blocks interaction with the underlying page.
 * By default only application actions close the popup. Enable
 * {@link #setCloseOnOutsideClick(boolean)} to remove it from the server widget tree when
 * the backdrop is clicked or tapped. Clicking its contents does not dismiss it.
 * The window and its backdrop are hidden and shown together. Set an accessible name and optional
 * description for screen readers. The frontmost modal owns keyboard focus until it closes.
 */
public class ModalPopup extends Popup
        implements HasBackdropColor, HasCloseOnOutsideClick, HasHiddenState, HasAccessibleName {
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

    /**
     * Checks whether Escape may dismiss this modal. Disabled by default.
     * @return true if Escape closes the modal
     */
    public final boolean isCloseOnEscape() {
        return this.getModel(State.ANY, Property.CLOSE_ON_ESCAPE).getData();
    }

    /**
     * Enables Escape dismissal without changing backdrop dismissal.
     * @param enabled true to enable Escape
     */
    public final void setCloseOnEscape(final boolean enabled) {
        this.getModel(State.ANY, Property.CLOSE_ON_ESCAPE).setData(enabled);
    }

    /**
     * Returns the plain-text description model.
     * @return description model
     */
    public final Model<String> getAccessibleDescriptionModel() {
        return this.getModel(State.ANY, Property.ACCESSIBLE_DESCRIPTION);
    }

    /**
     * Assigns a plain-text description; empty text removes it.
     * @param description accessible description
     */
    public final void setAccessibleDescription(final String description) {
        this.getAccessibleDescriptionModel().setData(description);
    }

    @Override
    public String getType() {
        return "modal popup";
    }
}
