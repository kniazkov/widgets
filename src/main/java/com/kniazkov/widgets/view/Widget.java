/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;
import com.kniazkov.widgets.model.CascadingModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.ModelBinding;
import com.kniazkov.widgets.model.ModelFactory;
import com.kniazkov.widgets.model.ModelListener;
import com.kniazkov.widgets.protocol.CreateWidget;
import com.kniazkov.widgets.protocol.RemoveChild;
import com.kniazkov.widgets.protocol.Subscribe;
import com.kniazkov.widgets.protocol.Update;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Abstract base class for all UI widgets. A widget represents a single element in the view tree.
 * Each widget has a unique {@link UId} and maintains a list of pending {@link Update}s that
 * describe how the client should create or modify its representation.
 * On creation, every widget automatically generates a {@link CreateWidget} update with its type.
 */
public abstract class Widget extends Entity {
    /**
     * Widget unique Id.
     */
    private final UId id;

    /**
     * Widget style.
     */
    private Style style;

    /**
     * The parent container that contains this widget.
     */
    private Container parent;

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
     * Constructs a new widget and schedules a {@link CreateWidget} update.
     */
    public Widget() {
        this.id = UId.create();
        this.style = EmptyStyle.INSTANCE;
        this.pushUpdate(new CreateWidget(this.id, this.getType()));
        this.bindings = new EnumMap<>(Property.class);
        this.stateBindings = new EnumMap<>(Property.class);
    }

    @Override
    public UId getId() {
        return this.id;
    }

    /**
     * Sets a new widget style.
     *
     * @param style new widget style
     */
    protected void setStyle(final Style style) {
        this.style = style;
    }

    /**
     * Returns the type of this widget, such as {@code "button"} or {@code "input field"}.
     *
     * @return the widget type
     */
    public abstract String getType();

    /**
     * Handles an event received from the client for this widget.
     *
     * @param type the event type
     * @param data optional event data
     */
    public abstract void handleEvent(final String type, final Optional<JsonObject> data);

    /**
     * Returns the container that currently holds this widget, if any.
     *
     * @return an {@link Optional} containing the parent container, or empty if the widget
     *  has no parent
     */
    public Optional<Container> getParent() {
        return Optional.ofNullable(this.parent);
    }

    /**
     * Removes this widget from its parent container.
     * This effectively removes the widget from the UI hierarchy. After removal, this widget
     * is considered "detached" - it will no longer receive updates or events from the client.
     */
    public void remove() {
        this.setParent(null);
    }

    /**
     * Derives a binding for this style from a binding defined in a prototype. This method creates
     * a new {@link ModelBinding} that mirrors the behavior of the given binding but associates
     * it with this widget’s entity context. Derived model supports cascading updates
     * through {@link CascadingModel}.
     *
     * @param parent the binding to derive from
     * @param <T> the type of data managed by the binding
     * @return a new derived binding for this style
     */
    private <T> ModelBinding<T> deriveBinding(final ModelBinding<T> parent) {
        final ModelListener<T> listener = parent.getListener().create(this);
        final ModelFactory<T> factory = parent.getFactory();
        final Model<T> model = new CascadingModel<>(parent.getModel(), factory);
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
            result = this.deriveBinding(this.style.getBinding(property));
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
            result = this.deriveBinding(this.style.getBinding(property, state));
            subset.put(state, result);
        } else {
            result = (ModelBinding<T>) binding;
        }
        return result;
    }

    /**
     * Detaches this widget from all bound models.
     * After detachment, all models are effectively disconnected from the widget’s
     * listeners, allowing the garbage collector to reclaim the widget and its
     * associated bindings safely. The binding objects themselves remain intact and
     * can later reattach to new models if needed.
     */
    public void detach() {
        for (ModelBinding<?> binding : this.bindings.values()) {
            binding.detach();
        }
        for (Map<WidgetState, ModelBinding<?>> stateMap : this.stateBindings.values()) {
            for (ModelBinding<?> binding : stateMap.values()) {
                binding.detach();
            }
        }
    }

    /**
     * Sets the parent container of this widget.
     * <p>
     * If the widget was previously attached to another container, a {@link RemoveChild}
     * update is queued to notify the client about the removal of that relationship.
     * Afterward, the parent reference is updated to the new container.
     * </p>
     *
     * <b>Handling pending updates</b>
     * <p>
     * When a widget (or an entire subtree of widgets) has been detached from the hierarchy,
     * it may still have accumulated updates — for example, content changes or style updates.
     * Such updates are not sent to the client immediately because the widget is not part
     * of the active UI tree at that time.
     * </p>
     *
     * <p>
     * However, when reattaching that widget later, all previously accumulated updates
     * become <em>stale</em>: their {@link UId} values are older than those already processed
     * by the client. The client will reject them to preserve strict chronological consistency,
     * which would lead to a desynchronized state.
     * </p>
     *
     * <b>Solution</b>
     * <p>
     * To prevent this inconsistency, this method performs the following steps:
     * </p>
     * <ol>
     *   <li>Collects all pending updates from this widget and all its descendants
     *       (if it is a {@link Container}) using a depth-first traversal;</li>
     *   <li>Clones each update using {@link Update#clone()} to assign
     *       a fresh unique identifier;</li>
     *   <li>Adds these newly cloned updates to the outgoing update queue.</li>
     * </ol>
     *
     * <p>
     * As a result, the client receives a valid, up-to-date sequence of updates:
     * the widget (and its subtree) are created and displayed as if they had just
     * been constructed on the server at the time of attachment.
     * </p>
     *
     * @param container the new parent container (may be {@code null})
     */
    void setParent(final Container container) {
        if (container == null) {
            if (this.parent != null) {
                this.pushUpdate(new RemoveChild(this.id, this.parent.getId()));
            }
            this.parent = null;
            return;
        }
        Set<Update> pending = new TreeSet<>();
        if (this instanceof Container) {
            for (final Widget child : (Container)this) {
                child.getUpdates(pending);
            }
        } else {
            this.getUpdates(pending);
        }
        if (this.parent != null) {
            this.pushUpdate(new RemoveChild(this.id, this.parent.getId()));
        }
        for (Update update : pending) {
            this.pushUpdate(update.clone());
        }
        this.parent = container;
    }

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
        this.bindings.put(property, new ModelBinding<>(model, listener, factory));
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
            .put(state, new ModelBinding<>(model, listener, factory));
    }

    /**
     * Adds a {@link Subscribe} update to listen for the specified event.
     *
     * @param event the event type to subscribe to
     */
    protected void subscribeToEvent(final String event) {
        this.pushUpdate(new Subscribe(this.id, event));
    }
}
