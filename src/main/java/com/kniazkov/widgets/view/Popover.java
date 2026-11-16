/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * An interactive nonmodal panel anchored to a native button. Click toggles; Escape and
 * outside pointer presses close it. Focus enters on open and returns on Escape/explicit close.
 */
public final class Popover extends AnchoredOverlay {
    /**
     * @param anchor native trigger, mounted in the same page/modal
     * @param name nonblank accessible dialog name
     */
    public Popover(final Button anchor, final String name) {
        super(anchor);
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Popover needs an accessible name");
        }
        this.setAccessibleName(name);
        this.publishOverlay();
    }

    /**
     * @param widget new owned interactive content, retained between openings
     */
    public void addBody(final BlockWidget<?> widget) {
        this.addContent(widget);
    }

    @Override
    public String getType() {
        return "anchored popover";
    }

    @Override
    protected String overlayKind() {
        return "popover";
    }
}
