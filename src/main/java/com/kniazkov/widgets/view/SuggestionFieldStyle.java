/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import java.util.List;

/**
 * Input field styling with an initially empty suggestion model.
 */
public class SuggestionFieldStyle extends InputFieldStyle implements HasSuggestions {
    /**
     * Default suggestion field style.
     */
    public static final SuggestionFieldStyle DEFAULT = new SuggestionFieldStyle();

    /**
     * Initializes standard input styling and an empty suggestion list.
     */
    private SuggestionFieldStyle() {
        super(InputFieldStyle.DEFAULT);
        this.setSuggestions(List.of());
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
