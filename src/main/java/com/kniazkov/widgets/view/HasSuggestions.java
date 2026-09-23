/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;
import java.util.List;

/**
 * An entity whose suggestions are supplied by a replaceable list model.
 */
public interface HasSuggestions extends Entity {
    /**
     * Returns the ordered suggestion model.
     * @return suggestion model
     */
    default Model<List<String>> getSuggestionsModel() {
        return this.getModel(State.ANY, Property.SUGGESTIONS);
    }

    /**
     * Binds an application-owned model. Values must be non-null lists of non-null strings.
     * @param model suggestion model, optionally shared between fields
     */
    default void setSuggestionsModel(final Model<List<String>> model) {
        this.setModel(State.ANY, Property.SUGGESTIONS, model);
    }

    /**
     * Returns the current suggestions.
     * @return ordered suggestions
     */
    default List<String> getSuggestions() {
        return this.getSuggestionsModel().getData();
    }

    /**
     * Replaces the suggestions without changing the entered text.
     * @param values ordered suggestions
     */
    default void setSuggestions(final List<String> values) {
        this.getSuggestionsModel().setData(List.copyOf(values));
    }
}
