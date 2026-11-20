/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Visible native label for a text editor in the same page.
 */
public final class Label extends TextWidget {
    /**
     * Constructor.
     * @param text visible, reactive label text
     */
    public Label(final String text) {
        super(text);
    }

    /**
     * Associates the label with an editor; clicking it moves focus natively.
     * @param editor editor, or null to unlink
     */
    public void setTarget(final InputField editor) {
        this.setAccessibleReference(Property.LABEL_TARGET, editor);
    }

    @Override
    public String getType() {
        return "label";
    }
}
