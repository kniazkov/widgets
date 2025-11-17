/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.DefaultModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.SynchronizedModel;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a hierarchical style that defines a set of reactive {@link Model} values
 * associated with specific {@link State} and {@link Property} keys.
 * <p>
 * Each style may have an optional parent. When a child style is created from a parent,
 * it inherits all models from the parent using {@link Model#asCascading()},
 * so that changes in the parent propagate to children until overridden locally.
 */
public abstract class Style implements Entity {
    /**
     * Returns empty state implementation.
     *
     * @return an empty state
     */
    public static Style getEmptyStyle() {
        return EmptyStyle.INSTANCE;
    }


    /**
     * Two-dimensional mapping: {@link State} → ({@link Property} → {@link SynchronizedModel}).
     * <p>
     * Each entry defines a reactive, thread-safe model representing a specific visual or
     * behavioral property (for example, color, size, font, opacity) in a given control state
     * such as {@code NORMAL}, {@code HOVER}, or {@code DISABLED}.
     * <p>
     * All models are wrapped in {@link SynchronizedModel} to ensure consistent concurrent access
     * and safe update propagation across multiple UI components or client threads that may
     * observe or modify the same style instance simultaneously.
     */
    private final Map<State, Map<Property<?>, SynchronizedModel<?>>> models;

    /**
     * Creates an empty style instance intended for subclass initialization.
     */
    protected Style() {
        this.models = new EnumMap<>(State.class);
    }

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
        parent.forEachModel((state, property, model) -> {
            Map<Property<?>, SynchronizedModel<?>> subset =
                this.models.computeIfAbsent(state, s -> new HashMap<>());
            subset.put(property, model.asCascading().asSynchronized());
        });
    }

    /**
     * A functional interface representing an operation that accepts a single
     * reactive {@link Model} along with its associated {@link State} and {@link Property}.
     *
     * @see Style#forEachModel(ModelConsumer)
     */
    @FunctionalInterface
    public interface ModelConsumer {
        /**
         * Performs an operation on the given model bound to a specific
         * {@link State} and {@link Property}.
         *
         * @param state the logical state of the style element (e.g. normal, hovered, disabled)
         * @param property the property key identifying the visual or behavioral attribute
         * @param model the reactive model associated with this state/property pair
         */
        void accept(State state, Property<?> property, Model<?> model);
    }

    /**
     * Iterates over all model bindings stored in this style and invokes the specified callback
     * for each ({@link State}, {@link Property}, {@link Model}) triple.
     *
     * @param action the callback to invoke for each model; must not be {@code null}
     * @throws NullPointerException if {@code action} is {@code null}
     */
    public void forEachModel(final ModelConsumer action) {
        for (Map.Entry<State, Map<Property<?>, SynchronizedModel<?>>> stateRecord
                : this.models.entrySet()) {
            final State state = stateRecord.getKey();
            final Map<Property<?>, SynchronizedModel<?>> subset = stateRecord.getValue();
            if (subset.isEmpty()) {
                continue;
            }
            for (Map.Entry<Property<?>, SynchronizedModel<?>> propRecord : subset.entrySet()) {
                final Property<?> property = propRecord.getKey();
                final Model<?> model = propRecord.getValue();
                action.accept(state, property, model);
            }
        }
    }

    /**
     * Returns the reactive model associated with the specified {@link State} and {@link Property}.
     * <p>
     * The returned instance is always a {@link SynchronizedModel} wrapper around the actual
     * underlying model. This wrapper provides:
     * <ul>
     *   <li><b>Thread safety</b> — all access and notifications are synchronized.</li>
     *   <li><b>Stable identity</b> — even if the underlying model changes later,
     *       the returned wrapper remains the same instance.</li>
     *   <li><b>Automatic propagation</b> — replacing the base model automatically
     *       updates all listeners observing this wrapper.</li>
     * </ul>
     * As a result, UI widgets or other components can safely subscribe to models returned by this
     * method and continue receiving updates even when the style’s internal models change.
     *
     * @param state the logical state (e.g. normal, hovered, disabled)
     * @param property the property to retrieve
     * @param <T> the type of data managed by the model
     * @return the synchronized model wrapper for the given state and property
     * @throws IllegalArgumentException if the type does not match the property or the state is
     *  not supported
     */
    @Override
    public <T> Model<T> getModel(final State state, final Property<T> property) {
        if (state != State.ANY && !getSupportedStates().contains(state)) {
            throw new IllegalArgumentException("Unsupported state: " + state);
        }
        return this.getBinding(state, property);
    }

    /**
     * Replaces the underlying model for the specified {@link State} and {@link Property}
     * while preserving the synchronized binding and all existing listener connections.
     * <p>
     * The specified model becomes the new base for the corresponding
     * {@link SynchronizedModel} wrapper. All observers currently subscribed to that wrapper
     * automatically start receiving updates from the new model without any re-subscription.
     * <p>
     * This mechanism enables seamless model replacement across a deep reactive hierarchy:
     * even widgets several layers down (through multiple cascading styles) continue to receive
     * updates transparently after the model change.
     *
     * <b>Key Benefits</b>
     * <ul>
     *   <li>The wrapper itself never changes — clients keep their references valid.</li>
     *   <li>All updates from the new model are re-emitted through the same synchronized layer.</li>
     *   <li>No memory leaks or race conditions: access remains thread-safe, and stale listeners
     *       are managed via weak references inside {@link SynchronizedModel}.</li>
     * </ul>
     *
     * @param state the logical state (e.g. normal, hovered, disabled)
     * @param property the property to update
     * @param model the new base model to assign (must not be {@code null})
     * @param <T> the type of data managed by the model
     * @throws IllegalArgumentException if the type does not match the property or the state is
     *  not supported
     */
    @Override
    public <T> void setModel(final State state, final Property<T> property, final Model<T> model) {
        if (state != State.ANY && !getSupportedStates().contains(state)) {
            throw new IllegalArgumentException("Unsupported state: " + state);
        }
        final SynchronizedModel<T> binding = this.getBinding(state, property);
        binding.setBase(model);
    }

    /**
     * Creates a derived style based on this style instance.
     * <p>
     * A derived style inherits all reactive model bindings from the current style
     * using {@link Model#asCascading()} semantics. This means:
     * <ul>
     *   <li>all properties and states are initially linked to the parent’s models,</li>
     *   <li>changes in this style automatically propagate to the derived one
     *       until the derived style overrides specific properties locally,</li>
     *   <li>all inherited models are wrapped in {@link SynchronizedModel} to ensure
     *       thread-safe concurrent access across multiple components.</li>
     * </ul>
     * <p>
     * Concrete subclasses must implement this method to return a new instance of their own type,
     * typically by invoking their constructor that accepts
     * a parent {@code Style}.
     *
     * @return a new style instance that inherits from this one
     */
    public abstract Style derive();

    /**
     * Returns a typed, thread-safe {@link SynchronizedModel} for the given {@link State}
     * and {@link Property}, automatically creating it if missing.
     * If a model is present but its data type is incompatible with the expected
     * {@code type}, an {@link IllegalArgumentException} is thrown.
     *
     * @param state the logical state (e.g. NORMAL, DISABLED)
     * @param property the property key (e.g. TEXT, COLOR, WIDTH)
     * @param <T> the type parameter of the model’s data
     * @return an existing or newly created {@link SynchronizedModel} of the correct type
     * @throws IllegalArgumentException if an existing model has an incompatible type
     */
    private <T> SynchronizedModel<T> getBinding(final State state, final Property<T> property) {
        Map<Property<?>, SynchronizedModel<?>> subset =
            this.models.computeIfAbsent(state, k -> new HashMap<>());
        final SynchronizedModel<?> model = subset.get(property);
        if (model == null) {
            Model<T> defaultModel = property.createDefaultModel();
            SynchronizedModel<T> sync = new SynchronizedModel<>(defaultModel);
            subset.put(property, sync);
            return sync;
        } else {
            return property.cast(model);
        }
    }

    /**
     * A minimal no-op implementation of {@link Style} that provides default, standalone models
     * but does not store or propagate any state.
     * This style serves as a safe fallback or placeholder for widgets or components
     * that require a style reference but do not yet have a concrete one assigned.
     */
    private static class EmptyStyle extends Style {
        /**
         * The singleton instance of the empty style.
         */
        private static final Style INSTANCE = new EmptyStyle();

        @Override
        public <T> Model<T> getModel(final State state, final Property<T> property) {
            return property.createDefaultModel();
        }

        @Override
        public <T> void setModel(final State state, final Property<T> property,
                final Model<T> model) {
            // do nothing
        }

        @Override
        public EmptyStyle derive() {
            return this;
        }
    }
}
