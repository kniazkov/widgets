/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import java.util.List;

/**
 * Editable text with optional suggestions. Arbitrary text is always allowed.
 * Suggestions are filtered by case-insensitive substring in the browser; choosing one
 * produces the same text-input event and model update as typing.
 * The application owns the suggestion history: typing never adds values to it.
 */
public class SuggestionField extends InputField implements HasSuggestions {
    /**
     * Returns the default style.
     * @return default suggestion field style
     */
    public static SuggestionFieldStyle getDefaultStyle() {
        return SuggestionFieldStyle.DEFAULT;
    }

    /**
     * Creates an empty field with no suggestions.
     */
    public SuggestionField() {
        this(getDefaultStyle(), "");
    }

    /**
     * Creates an empty field with the supplied suggestions.
     * @param suggestions ordered suggestions
     */
    @SuppressWarnings("this-escape")
    public SuggestionField(final List<String> suggestions) {
        this();
        this.setSuggestions(suggestions);
    }

    /**
     * Creates a styled field with initial text.
     * @param style field style
     * @param text initial text
     */
    public SuggestionField(final SuggestionFieldStyle style, final String text) {
        super(style, text);
    }

    @Override
    public String getType() {
        return "suggestion field";
    }
}
