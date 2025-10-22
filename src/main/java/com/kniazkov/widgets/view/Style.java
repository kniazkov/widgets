/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;
import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a hierarchical style that defines a set of reactive {@link Model} values
 * associated with specific {@link State} and {@link Property} keys.
 * <p>
 * Each style may have an optional parent. When a child style is created from a parent,
 * it inherits all models from the parent using {@link Model#asCascading()},
 * so that changes in the parent propagate to children until overridden locally.
 * <p>
 */
public abstract class Style {
    /**
     * Two-dimensional mapping: {@link State} → ({@link Property} → {@link Model}).
     * Each model may represent a reactive property, such as color, size, or font,
     * whose value can depend on the control’s state (e.g. normal, hovered, disabled).
     */
    private final Map<State, Map<Property, Model<?>>> models;

    /**
     * Creates a new style with parent inheritance.
     * If {@code parent} is non-null, this constructor automatically copies all models
     * from the parent style using {@link Model#asCascading()}, so that the new style
     * can override individual properties without breaking the link to its base values.
     *
     * @param parent the parent style to inherit from
     */
    public Style(final Style parent) {
        this.models = new EnumMap<>(State.class);

        for (Map.Entry<State, Map<Property, Model<?>>> stateEntry : parent.models.entrySet()) {
            final State state = stateEntry.getKey();
            final Map<Property, Model<?>> src = stateEntry.getValue();
            if (src.isEmpty()) {
                continue;
            }
            final Map<Property, Model<?>> dst = new EnumMap<>(Property.class);
            for (Map.Entry<Property, Model<?>> propEntry : src.entrySet()) {
                final Property property = propEntry.getKey();
                final Model<?> model = propEntry.getValue();
                dst.put(property, model.asCascading());
            }
            this.models.put(state, dst);
        }
    }
}
