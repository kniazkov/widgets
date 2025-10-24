/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.common.UId;
import com.kniazkov.widgets.model.Binding;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.SynchronizedModel;
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
public abstract class Widget implements Entity {
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
     * Two-dimensional mapping: {@link State} → ({@link Property} → {@link Binding}).
     * Each entry defines a live connection between a reactive {@link Model} and
     * a {@link Listener} that responds to model updates for a specific
     * visual or behavioral {@link Property} in a given {@link State}.
     */
    private final Map<State, Map<Property, Binding<?>>> bindings;

    /**
     * Creates a new widget instance initialized from the specified {@link Style}.
     *
     * @param style the style providing the initial models and properties for this widget
     */
    public Widget(final Style style) {
        this.id = UId.create();
        this.updates = new ArrayList<>();
        this.updates.add(new CreateWidget(this.id, this.getType()));
        this.bindings = new EnumMap<>(State.class);
        style.forEachModel((state, property, model) -> {
            Map<Property, Binding<?>> subset =
                this.bindings.computeIfAbsent(state, s -> new EnumMap<>(Property.class));
            subset.put(
                property,
                property.bindModel(
                    state,
                    model.asCascading(),
                    this
                )
            );
        });
    }

    @Override
    public <T> Model<T> getModel(final State state, final Property property, final Class<T> type) {
        return this.getBinding(state, property, type).getModel();
    }

    @Override
    public <T> void setModel(final State state, final Property property, final Class<T> type,
            final Model<T> model) {
        final Binding<T> binding = this.getBinding(state, property, type);
        binding.setModel(model);
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
     * Registers a reactive binding between a {@link Model} and a {@link Listener}
     * for the specified {@link State} and {@link Property}.
     * <p>
     * This method is intended to be used by subclass constructors to define how
     * this widget reacts to model updates — for example, by updating visual attributes
     * such as color, size, or text when the model’s data changes.
     *
     * @param state the logical state of the widget (e.g. normal, hovered, disabled)
     * @param property the visual or behavioral property being bound
     * @param model the reactive model providing data
     * @param listener the listener that responds to model updates
     * @param <T> the type of data managed by the model
     */
    protected <T> void bindModel(final State state, final Property property, final Model<T> model,
            final Listener<T> listener) {
        Map<Property, Binding<?>> subset =
            this.bindings.computeIfAbsent(state, s -> new EnumMap<>(Property.class));
        subset.put(property, new Binding<>(model, listener));
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

    /**
     * Returns a typed {@link Binding} for the given {@link State} and {@link Property},
     * automatically creating it if missing.
     *
     * @param state the widget’s current logical state (e.g. NORMAL, HOVERED)
     * @param property the property key (e.g. TEXT, COLOR)
     * @param type the expected data type of the bound model
     * @param <T> the type parameter of the model’s data
     * @return an existing or newly created {@link Binding} for the given property and state
     * @throws IllegalArgumentException if the existing binding has an incompatible model type
     */
    private <T> Binding<T> getBinding(final State state, final Property property,
            final Class<T> type) {
        Map<Property, Binding<?>> subset = this.bindings.get(state);
        if (subset == null) {
            subset = new EnumMap<>(Property.class);
            this.bindings.put(state, subset);
        }
        Binding<?> binding = subset.get(property);
        if (binding == null) {
            final Model<?> defaultModel = property.createDefaultModel();
            binding = property.bindModel(state, defaultModel, this);
            subset.put(property, binding);
        }
        final Object data = binding.getModel().getData();
        if (data != null && !type.isInstance(data)) {
            throw new IllegalArgumentException(
                "Binding for " + property + " in state " + state +
                    " has incompatible type: " + data.getClass().getName() +
                    " (expected " + type.getName() + ")"
            );
        }
        @SuppressWarnings("unchecked")
        final Binding<T> typed = (Binding<T>) binding;
        return typed;
    }
}
