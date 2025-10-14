/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.ModelBinding;

/**
 * Represents a visual style applied to a widget.
 * A {@code Style} defines a collection of {@link Model} instances that describe
 * various visual properties such as colors, fonts, borders, etc.
 * Each concrete style type defines its own set of supported properties.
 * Styles are typically hierarchical: a derived style may inherit all properties
 * from a base style while overriding selected ones.
 */
public interface Style {
    /**
     * Returns the model binding associated with the specified property.
     *
     * @param property the property key
     * @return the model binding associated with the given property
     */
    <T> ModelBinding<T> getBinding(Property property);

    /**
     * Returns a state-dependent binding for the specified property and state.
     *
     * @param property property key
     * @param state widget state
     * @param <T> binding data type
     * @return the model binding
     * @throws IllegalStateException if no binding is found for the given state
     */
    <T> ModelBinding<T> getBinding(Property property, WidgetState state);

    /**
     * Creates a new style derived from this style.
     *
     * @return a new {@code Style} instance derived from this one
     */
    Style derive();
}
