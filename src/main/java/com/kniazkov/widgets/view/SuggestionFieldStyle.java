/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Style definition for an editable suggestion field.
 */
public class SuggestionFieldStyle extends InputFieldStyle {
    /**
     * Default suggestion field style.
     */
    public static final SuggestionFieldStyle DEFAULT = new SuggestionFieldStyle();

    /**
     * Initializes standard input styling.
     */
    private SuggestionFieldStyle() {
        super(InputFieldStyle.DEFAULT);
    }

    /**
     * Derives a style from an existing suggestion field style.
     * @param parent parent style
     */
    public SuggestionFieldStyle(final SuggestionFieldStyle parent) {
        super(parent);
    }

    @Override
    public SuggestionFieldStyle derive() {
        return new SuggestionFieldStyle(this);
    }
}
