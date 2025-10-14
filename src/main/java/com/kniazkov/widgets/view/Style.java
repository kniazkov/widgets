/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

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
     * Creates a new style derived from this style.
     *
     * @return a new {@code Style} instance derived from this one
     */
    Style derive();
}
