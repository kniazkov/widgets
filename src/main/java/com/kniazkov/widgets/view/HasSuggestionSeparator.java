/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * An entity with a reactive literal suggestion separator.
 * An empty separator enables whole-field suggestions.
 */
public interface HasSuggestionSeparator extends Entity {
    /**
     * Returns the model that stores the suggestion separator.
     *
     * @return suggestion separator model
     */
    default Model<String> getSuggestionSeparatorModel() {
        return this.getModel(State.ANY, Property.SUGGESTION_SEPARATOR);
    }

    /**
     * Sets the model that stores the suggestion separator.
     *
     * @param model suggestion separator model
     */
    default void setSuggestionSeparatorModel(final Model<String> model) {
        this.setModel(State.ANY, Property.SUGGESTION_SEPARATOR, model);
    }

    /**
     * Returns the current suggestion separator.
     *
     * @return suggestion separator
     */
    default String getSuggestionSeparator() {
        return this.getSuggestionSeparatorModel().getData();
    }

    /**
     * Updates the suggestion separator.
     *
     * @param separator new suggestion separator
     */
    default void setSuggestionSeparator(final String separator) {
        this.getSuggestionSeparatorModel().setData(separator);
    }
}
