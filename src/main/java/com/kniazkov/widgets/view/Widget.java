/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;
import com.kniazkov.widgets.model.ModelBinding;
import com.kniazkov.widgets.protocol.CreateWidget;
import com.kniazkov.widgets.protocol.RemoveChild;
import com.kniazkov.widgets.protocol.Subscribe;
import com.kniazkov.widgets.protocol.Update;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
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
public abstract class Widget {
    /**
     * Widget unique Id.
     */
    private final UId id;

    /**
     * List of updates not yet sent to the client.
     */
    private final List<Update> updates;

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
        this.updates = new ArrayList<>();
        this.updates.add(new CreateWidget(this.id, this.getType()));
        this.bindings = new EnumMap<>(Property.class);
        this.stateBindings = new EnumMap<>(Property.class);
    }

    /**
     * Returns the unique identifier of this widget.
     *
     * @return the widget ID
     */
    public UId getId() {
        return this.id;
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
     * Collects and clears all pending updates from this widget, adding them
     * to the given set. This effectively drains the update queues.
     *
     * @param set the set to which updates are added
     */
    public void getUpdates(final Set<Update> set) {
        set.addAll(this.updates);
        this.updates.clear();
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
    public <T> ModelBinding<T> getModelBinding(final Property property) {
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
    public <T> ModelBinding<T> getModelBinding(final Property property, final WidgetState state) {
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
                this.updates.add(new RemoveChild(this.id, this.parent.getId()));
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
            this.updates.add(new RemoveChild(this.id, this.parent.getId()));
        }
        for (Update update : pending) {
            this.updates.add(update.clone());
        }
        this.parent = container;
    }

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

    /**
     * Adds an update for this widget.
     *
     * @param update the update to add
     */
    protected void pushUpdate(final Update update) {
        this.updates.add(update);
    }

    /**
     * Adds a {@link Subscribe} update to listen for the specified event.
     *
     * @param event the event type to subscribe to
     */
    protected void subscribeToEvent(final String event) {
        this.updates.add(new Subscribe(this.id, event));
    }
}
