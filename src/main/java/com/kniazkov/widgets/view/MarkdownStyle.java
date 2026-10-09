/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Base typography for Markdown; document markup supplies relative internal formatting.
 * Inherits the text theme (16px by default), with independent cascading overrides.
 */
public class MarkdownStyle extends TextWidgetStyle
        implements HasMargin, HasPadding, HasHiddenState {
    /**
     * Shared default document style.
     */
    public static final MarkdownStyle DEFAULT = new MarkdownStyle();

    /**
     * Creates the default document style.
     */
    private MarkdownStyle() {
        super(TextWidgetStyle.DEFAULT);
        this.setMargin(0);
        this.setPadding(0);
        this.setHiddenFlag(false);
    }

    /**
     * Creates a style inheriting its parent's models.
     *
     * @param parent parent document style
     */
    public MarkdownStyle(final MarkdownStyle parent) {
        super(parent);
    }

    @Override
    public MarkdownStyle derive() {
        return new MarkdownStyle(this);
    }
}
