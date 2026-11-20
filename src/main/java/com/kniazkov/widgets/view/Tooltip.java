/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Noninteractive supplemental text shown on mouse hover or keyboard focus. The accessible
 * description remains available while visually closed. Do not put essential touch actions here.
 */
public final class Tooltip extends AnchoredOverlay {
    /**
     * Owned plain-text description.
     */
    private final TextWidget text;

    /**
     * @param anchor described widget; focus activation requires a focusable anchor
     * @param text supplemental plain text
     */
    public Tooltip(final Widget<?> anchor, final String text) {
        super(anchor);
        this.text = new TextWidget(text);
        this.addContent(new Section(this.text));
        this.publishOverlay();
    }

    /**
     * @param text replacement plain text
     */
    public void setText(final String text) {
        this.requireUsable();
        this.text.setText(java.util.Objects.requireNonNull(text, "text"));
    }

    /**
     * @param show opening delay in milliseconds, 0..5000
     * @param hide closing delay in milliseconds, 0..5000
     */
    public void setHoverDelays(final int show, final int hide) {
        this.setDelays(show, hide);
    }

    @Override
    public String getType() {
        return "anchored tooltip";
    }

    @Override
    protected String overlayKind() {
        return "tooltip";
    }
}
