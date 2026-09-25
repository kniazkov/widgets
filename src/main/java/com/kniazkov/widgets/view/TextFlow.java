/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * A section using normal inline text flow. Adjacent text and links wrap across
 * lines as one paragraph, including long unbroken words. Explicit spaces between
 * children are preserved as ordinary HTML whitespace. Atomic inline widgets
 * (buttons, images, inline blocks) retain their own layout.
 */
public final class TextFlow extends Section {
    /**
     * Creates an empty left-aligned flow with baseline alignment.
     */
    public TextFlow() {
        this.setLeftAlignment();
        this.setBaseLineAlignment();
    }

    @Override
    public String getType() {
        return "text flow";
    }
}
