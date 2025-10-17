/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.UId;
import com.kniazkov.widgets.model.CascadingModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.ModelBinding;
import com.kniazkov.widgets.model.ModelFactory;
import com.kniazkov.widgets.model.ModelListener;
import com.kniazkov.widgets.model.SynchronizedModel;
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
public abstract class Style extends Entity {
    /**
     * Unique identifier of this style.
     */
    private final UId id;

    /**
     * The prototype (base) style from which this style inherits properties.
     * When a property or state binding is not defined in this style, it is lazily
     * resolved by looking up the prototype. Derived styles can override any of
     * the prototype’s bindings while preserving shared defaults.
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

    /**
     * Creates a new style instance with the specified prototype.
     *
     * @param prototype the base style from which this one inherits, or {@code null}
     *  if this is a root style
     */
    protected Style(final Style prototype) {
        this.id = UId.create();
        this.prototype = prototype;
        this.bindings = new EnumMap<>(Property.class);
        this.stateBindings = new EnumMap<>(Property.class);
    }

    @Override
    public UId getId() {
        return this.id;
    }

    /**
     * Derives a binding for this style from a binding defined in a prototype. This method creates
     * a new {@link ModelBinding} that mirrors the behavior of the given parent binding but
     * associates it with this style’s entity context. Derived model is synchronized and supports
     * cascading updates through {@link CascadingModel}.
     *
     * @param parent the binding to derive from
     * @param <T> the type of data managed by the binding
     * @return a new derived binding for this style
     */
    private <T> ModelBinding<T> deriveBinding(final ModelBinding<T> parent) {
        final ModelListener<T> listener = parent.getListener().create(this);
        final ModelFactory<T> factory = parent.getFactory();
        final Model<T> model = new SynchronizedModel<>(
            new CascadingModel<>(parent.getModel(), factory)
        );
        return new ModelBinding<>(model, listener, factory);
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
        final ModelBinding<T> result;
        final ModelBinding<?> binding = this.bindings.get(property);
        if (binding == null) {
            if (this.prototype == null) {
                throw new IllegalStateException("No binding for property: " + property);
            }
            result = this.deriveBinding(this.prototype.getBinding(property));
            this.bindings.put(property, result);
        } else {
            result = (ModelBinding<T>) binding;
        }
        return result;
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
        final ModelBinding<T> result;
        final Map<WidgetState, ModelBinding<?>> subset = this.stateBindings
            .computeIfAbsent(property, k -> new EnumMap<>(WidgetState.class));
        final ModelBinding<?> binding = subset.get(state);
        if (binding == null) {
            if (this.prototype == null) {
                throw new IllegalStateException(
                    "No binding for property: " + property + " in state: " + state
                );
            }
            result = this.deriveBinding(this.prototype.getBinding(property, state));
            subset.put(state, result);
        } else {
            result = (ModelBinding<T>) binding;
        }
        return result;
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
     * @param model the initial model to bind
     * @param listener the listener that will receive model updates
     * @param factory  the factory used to lazily create models when needed
     * @param <T> binding data type
     */
    protected <T> void bind(
        final Property property,
        final Model<T> model,
        final ModelListener<T> listener,
        final ModelFactory<T> factory
    ) {
        this.bindings.put(
            property,
            new ModelBinding<>(new SynchronizedModel<>(model), listener, factory)
        );
    }

    /**
     * Adds a state-dependent binding for the specified property and state.
     *
     * @param property property key
     * @param state widget state
     * @param model the initial model to bind
     * @param listener the listener that will receive model updates
     * @param factory  the factory used to lazily create models when needed
     * @param <T> binding data type
     */
    protected <T> void bind(
        final Property property,
        final WidgetState state,
        final Model<T> model,
        final ModelListener<T> listener,
        final ModelFactory<T> factory
    ) {
        this.stateBindings
            .computeIfAbsent(property, k -> new EnumMap<>(WidgetState.class))
            .put(state, new ModelBinding<>(new SynchronizedModel<>(model), listener, factory));
    }
}
