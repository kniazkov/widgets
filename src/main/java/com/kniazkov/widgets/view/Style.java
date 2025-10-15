/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.ModelBinding;
import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a visual style applied to a widget.
 * A {@code Style} defines a collection of {@link Model} instances that describe
 * various visual properties such as colors, fonts, borders, etc.
 * Each concrete style type defines its own set of supported properties.
 * Styles are typically hierarchical: a derived style may inherit all properties
 * from a base style while overriding selected ones.
 */
public abstract class Style {
    /**
     * Prototype style. If there is no model in the current style, it searches in the prototype.
     */
    private final Style prototype;

    /**
     * Stores bindings that are independent of widget state.
     */
    private final Map<Property, ModelBinding<?>> bindings;

    /**
     * Stores bindings that depend on widget state.
     * Example: background color may vary between NORMAL, HOVER, ACTIVE, etc.
     */
    private final Map<Property, Map<WidgetState, ModelBinding<?>>> stateBindings;

    protected Style(final Style prototype) {
        this.prototype = prototype;
        this.bindings = new EnumMap<>(Property.class);
        this.stateBindings = new EnumMap<>(Property.class);
    }

    /**
     * Returns a state-independent binding for the specified property.
     *
     * @param property property key
     * @param <T> binding data type
     * @return the model binding
     * @throws IllegalStateException if no binding is found
     */
    @SuppressWarnings("unchecked")
    public <T> ModelBinding<T> getBinding(final Property property) {
        final ModelBinding<?> binding = this.bindings.get(property);
        if (binding == null) {
            throw new IllegalStateException("No binding for property: " + property);
        }
        return (ModelBinding<T>) binding;
    }

    /**
     * Returns a state-dependent binding for the specified property and state.
     *
     * @param property property key
     * @param state widget state
     * @param <T> binding data type
     * @return the model binding
     * @throws IllegalStateException if no binding is found for the given state
     */
    @SuppressWarnings("unchecked")
    public <T> ModelBinding<T> getBinding(final Property property, final WidgetState state) {
        final Map<WidgetState, ModelBinding<?>> byState = this.stateBindings.get(property);
        if (byState == null) {
            throw new IllegalStateException("No state bindings for property: " + property);
        }
        final ModelBinding<?> binding = byState.get(state);
        if (binding == null) {
            throw new IllegalStateException(
                "No binding for property: " + property + " in state: " + state
            );
        }
        return (ModelBinding<T>) binding;
    }

    /**
     * Creates a new style derived from this style.
     *
     * @return a new {@code Style} instance derived from this one
     */
    public abstract Style derive();

    /**
     * Adds a state-independent binding for the specified property.
     *
     * @param property property key
     * @param binding model binding
     * @param <T> binding data type
     */
    protected <T> void addBinding(final Property property, final ModelBinding<T> binding) {
        this.bindings.put(property, binding);
    }

    /**
     * Adds a state-dependent binding for the specified property and state.
     *
     * @param property property key
     * @param state widget state
     * @param binding model binding
     * @param <T> binding data type
     */
    protected <T> void addBinding(final Property property, final WidgetState state,
                                  final ModelBinding<T> binding) {
        this.stateBindings
            .computeIfAbsent(property, k -> new EnumMap<>(WidgetState.class))
            .put(state, binding);
    }
}
