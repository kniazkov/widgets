/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.client.OnClient;
import com.kniazkov.widgets.common.RMId;
import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.controller.HandlesEvents;
import com.kniazkov.widgets.model.Binding;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.protocol.CreateWidget;
import com.kniazkov.widgets.protocol.DisposeWidget;
import com.kniazkov.widgets.protocol.SetClientEventAction;
import com.kniazkov.widgets.protocol.SetProperty;
import com.kniazkov.widgets.protocol.Subscribe;
import com.kniazkov.widgets.protocol.Update;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.TreeMap;
import java.lang.ref.WeakReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for all UI widgets. A widget represents a single element in the view tree.
 * Each widget has a unique {@link RMId} and maintains a list of pending {@link Update}s that
 * describe how the client should create or modify its representation.
 * On creation, every widget automatically generates a {@link CreateWidget} update with its type.
 *
 * @param <S> Widget style
 */
public abstract class Widget<S extends Style> implements Entity, HandlesEvents, HasAccessibleName {
    /**
     * Widget unique Id.
     */
    private final RMId id;

    /**
     * Widget style.
     */
    private S style;

    /**
     * List of updates not yet sent to the client.
     */
    private List<Update> updates;

    /**
     * Size charged for this widget's pending updates.
     */
    private long bufferedBytes;

    /**
     * Whether some changes were dropped before collection.
     */
    private boolean bufferOverflow;

    /**
     * Default per-widget budget before the widget is attached to a page.
     */
    private static final com.kniazkov.widgets.base.ProtocolLimits DETACHED_LIMITS =
        com.kniazkov.widgets.base.ProtocolLimits.defaults();

    /**
     * The parent container that contains this widget.
     */
    private Container parent;

    /**
     * Identity of the first browser root to which this widget was exported.
     */
    private RMId ownerRootId;

    /**
     * Most recent lifecycle owner, without retaining an abandoned page.
     */
    private WeakReference<RootWidget> lifecycleOwner;

    /**
     * Tracks cleanup independently from browser export/reparent validation.
     *
     * @param root owning page
     */
    final synchronized void setLifecycleOwner(final RootWidget root) {
        if (!this.belongsTo(root)) {
            this.lifecycleOwner = new WeakReference<>(root);
        }
        this.setBufferOwner(root);
    }

    /**
     * Root charged for these updates only while physically mounted.
     */
    private WeakReference<RootWidget> bufferOwner;

    /**
     * Transfers the raw buffer's charge without retaining detached widgets.
     *
     * @param root mounted owner, null on detach
     */
    private synchronized void setBufferOwner(final RootWidget root) {
        final RootWidget old = this.bufferOwner == null ? null : this.bufferOwner.get();
        if (old == root) {
            return;
        }
        this.releaseBudget();
        this.bufferOwner = root == null ? null : new WeakReference<>(root);
        if (root != null && (this.bufferOverflow || this.updates != null
                && !root.retainBuffered(this.updates.size(), this.bufferedBytes))) {
            root.markBufferOverflow();
            this.updates = null;
            this.bufferedBytes = 0;
        }
    }

    /**
     * Returns this widget's pending charge to its mounted owner.
     */
    private void releaseBudget() {
        final RootWidget owner = this.bufferOwner == null ? null : this.bufferOwner.get();
        if (owner != null && this.updates != null) {
            owner.releaseBuffered(this.updates.size(), this.bufferedBytes);
        }
    }

    /**
     * @param root proposed owner
     * @return whether the page still owns this widget's cleanup
     */
    final boolean belongsTo(final RootWidget root) {
        return this.lifecycleOwner != null && this.lifecycleOwner.get() == root;
    }

    /**
     * Whether this widget is permanently retired.
     */
    private volatile boolean retired;

    /**
     * Server-side cleanup diagnostics.
     */
    private static final Logger LOGGER = Logger.getLogger(Widget.class.getName());

    /**
     * Resource actions owned exclusively by this widget.
     */
    private final List<Runnable> cleanup = new ArrayList<>();

    /**
     * Two-dimensional mapping: {@link State} → ({@link Property} → {@link Binding}).
     * Each entry defines a live connection between a reactive {@link Model} and
     * a {@link Listener} that responds to model updates for a specific
     * visual or behavioral {@link Property} in a given {@link State}.
     */
    private final Map<State, Map<Property<?>, Binding<?>>> bindings;

    /**
     * One-dimensional mapping: {@link Event} → {@link Controller}.
     * Each entry defines the active {@link Controller} instance that handles
     * a particular {@link Event} type for this widget.
     */
    private final Map<Event<?>, Controller<?>> controllers;

    /*
     * The protocol requires the concrete widget type before the first update is queued.
     */
    /**
     * Creates a new widget instance initialized from the specified {@link Style}.
     *
     * @param style the style providing the initial models and properties for this widget
     */
    @SuppressWarnings("this-escape")
    public Widget(final S style) {
        this.id = RMId.create();
        this.style = style;
        this.pushUpdate(new CreateWidget(this.id, this.getType()));
        this.bindings = new EnumMap<>(State.class);
        this.controllers = new HashMap<>();

        style.forEachModel((state, property, model) -> {
            final Map<Property<?>, Binding<?>> subset =
                this.bindings.computeIfAbsent(state, s -> new HashMap<>());
            subset.put(
                property,
                property.bindStyleModel(state, model, this)
            );
        });
    }

    @Override
    public Set<State> getSupportedStates() {
        return this.style.getSupportedStates();
    }

    @Override
    public <T> Model<T> getModel(final State state, final Property<T> property) {
        if (state != State.ANY && !getSupportedStates().contains(state)) {
            throw new IllegalArgumentException("Unsupported state: " + state);
        }
        return this.getBinding(state, property).getModel();
    }

    @Override
    public <T> void setModel(final State state, final Property<T> property, final Model<T> model) {
        if (state != State.ANY && !getSupportedStates().contains(state)) {
            throw new IllegalArgumentException("Unsupported state: " + state);
        }
        final Binding<T> binding = this.getBinding(state, property);
        binding.setModel(model);
    }

    /**
     * Returns the widget supplying semantics for this control or composite.
     * @return this widget or a native control owned by it
     */
    protected Widget<?> accessibleTarget() {
        return this;
    }

    @Override
    public Model<String> getAccessibleNameModel() {
        return this.accessibleTarget().getModel(State.ANY, Property.ACCESSIBLE_NAME);
    }

    @Override
    public void setAccessibleNameModel(final Model<String> model) {
        this.accessibleTarget().setModel(State.ANY, Property.ACCESSIBLE_NAME, model);
    }

    /**
     * Returns the model storing the optional plain-text accessible description.
     * @return description model
     */
    public final Model<String> getAccessibleDescriptionModel() {
        return this.accessibleTarget().getModel(State.ANY, Property.ACCESSIBLE_DESCRIPTION);
    }

    /**
     * Binds the accessible description to application state.
     * @param model description model
     */
    public final void setAccessibleDescriptionModel(final Model<String> model) {
        this.accessibleTarget().setModel(State.ANY, Property.ACCESSIBLE_DESCRIPTION, model);
    }

    /**
     * Sets the plain-text description, or clears it with an empty string.
     * @param description accessible description
     */
    public final void setAccessibleDescription(final String description) {
        this.getAccessibleDescriptionModel().setData(description);
    }

    /**
     * Names this widget from another widget on the same page.
     * @param label label widget, or null to restore the explicit or native name
     */
    public final void setAccessibleLabel(final Widget<?> label) {
        this.setAccessibleReference(Property.ACCESSIBLE_LABEL, label);
    }

    /**
     * Associates a help widget with this widget's accessible description.
     * @param description description widget, or null to clear the relationship
     */
    public final void setAccessibleDescriptionWidget(final Widget<?> description) {
        this.setAccessibleReference(Property.ACCESSIBLE_DESCRIPTION_WIDGET, description);
    }

    /**
     * Associates a message with this widget while it is invalid.
     * @param error error message widget, or null to clear the relationship
     */
    public final void setAccessibleError(final Widget<?> error) {
        this.setAccessibleReference(Property.ACCESSIBLE_ERROR, error);
    }

    /**
     * Serializes the stable identity without retaining the target as a Java object.
     * @param property relationship property
     * @param target referenced widget, or null
     */
    private void setAccessibleReference(final Property<String> property, final Widget<?> target) {
        final Widget<?> source = this.accessibleTarget();
        if (target == this || target == source) {
            throw new IllegalArgumentException("The label target cannot be its own source");
        }
        source.getModel(State.ANY, property).setData(
            target == null ? "" : target.getId().toString()
        );
    }

    @Override
    public <T> Controller<T> getController(final Event<T> event) {
        final Controller<?> ctrl = this.controllers.get(event);
        if (ctrl == null) {
            Controller<T> stub = Controller.stub();
            this.controllers.put(event, stub);
            return stub;
        } else {
            @SuppressWarnings("unchecked")
            Controller<T> typed = (Controller<T>) ctrl;
            return typed;
        }
    }

    @Override
    public <T> void setController(final Event<T> event, final Controller<T> controller) {
        this.controllers.put(event, controller);
    }

    /**
     * Checks server-side hidden/disabled flags on this widget and its current ancestors.
     * This read does not create property bindings or enqueue updates.
     * @return whether interaction is currently allowed
     */
    public final boolean isInteractionEnabled() {
        final Map<Property<?>, Binding<?>> values = this.bindings.get(State.ANY);
        if (values != null) {
            for (final Property<Boolean> flag : java.util.List.of(Property.HIDDEN,
                    Property.DISABLED)) {
                final Binding<?> binding = values.get(flag);
                if (binding != null && Boolean.TRUE.equals(binding.getModel().getData())) {
                    return false;
                }
            }
        }
        return !(this.parent instanceof Widget<?> ancestor) || ancestor.isInteractionEnabled();
    }

    @Override
    public void handleEvent(final Event<?> event, final JsonObject object) {
        event.process(this, object);
    }

    @Override
    public void subscribeToEvent(final Event<?> event) {
        this.pushUpdate(new Subscribe(this.id, event.getName()));
    }

    @Override
    public void executeOnClient(final Event<?> event, final OnClient action) {
        this.pushUpdate(new SetClientEventAction(this.id, event.getName(), action));
    }

    /**
     * Returns the unique identifier of this widget.
     *
     * @return the widget ID
     */
    public RMId getId() {
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
     * @param object event data
     */
    public void handleEvent(final String type, final JsonObject object) {
        this.handleEvent(Event.getByName(type), object);
    }

    /**
     * Removes this widget from its parent container.
     * This effectively removes the widget from the UI hierarchy. After removal, this widget
     * is considered "detached" - it will no longer receive updates or events from the client.
     */
    public void remove() {
        if (this.parent != null) {
            this.parent.remove(this);
        }
    }

    /**
     * Removes and permanently releases this attached widget's browser subtree.
     * The retired instance and its descendants must not be reattached. Use
     * {@link #remove()} instead when the application will reuse the same instance.
     * This method does not close application-owned model subscriptions.
     */
    public final void dispose() {
        if (this instanceof RootWidget) {
            throw new IllegalStateException("The page owns its root widget lifecycle");
        }
        if (!this.retired) {
            final Optional<RootWidget> root = this.getRootWidget();
            this.remove();
            this.visitSubtree(Widget::retire);
            root.ifPresent(owner -> owner.pushUpdate(new DisposeWidget(this.id)));
        }
    }

    /**
     * Adds cleanup for one subscription or resource owned by this widget.
     *
     * @param action cleanup action
     */
    protected final synchronized void onRetire(final Runnable action) {
        this.requireUsable();
        this.cleanup.add(Objects.requireNonNull(action));
    }

    /**
     * @return whether this widget is no longer active
     */
    protected final boolean isRetired() {
        return this.retired;
    }

    /**
     * Rejects configuration that would reopen a retired widget's resources.
     */
    protected final void requireUsable() {
        if (this.retired) {
            throw new IllegalStateException("Widget is disposed");
        }
    }

    /**
     * Closes owned model listeners exactly once, without holding the widget monitor
     * when calling custom resource closers.
     */
    final void retire() {
        final List<Runnable> actions;
        synchronized (this) {
            if (this.retired) {
                return;
            }
            this.retired = true;
            this.controllers.clear();
            this.releaseBudget();
            this.updates = null;
            this.bufferedBytes = 0;
            actions = new ArrayList<>(this.cleanup);
            this.cleanup.clear();
            this.bindings.values().forEach(properties -> {
                properties.values().forEach(binding -> actions.add(binding::close));
                properties.clear();
            });
        }
        for (final Runnable action : actions) {
            try {
                action.run();
            } catch (final RuntimeException failure) {
                LOGGER.log(Level.WARNING, "Widget cleanup failed", failure);
            }
        }
    }

    /**
     * Collects and clears all pending updates from this widget, adding them
     * to the given set. This effectively drains the update queues.
     *
     * @param set the set to which updates are added
     */
    public void getUpdates(final Set<Update> set) {
        final List<Update> pending;
        synchronized (this) {
            if (this.updates == null) {
                return;
            }
            pending = this.updates;
            this.releaseBudget();
            this.updates = null;
            this.bufferedBytes = 0;
        }
        set.addAll(pending);
    }

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
     * Returns the root widget, recursively traversing all widgets up the parent chain.
     *
     * @return a reference to the root widget or an empty {@link Optional} if the given widget
     *  is not in the widget tree and, accordingly, the root widget is not accessible
     */
    public Optional<RootWidget> getRootWidget() {
        if (!(this.parent instanceof Widget<?> parentWidget)) {
            return Optional.empty();
        }
        return parentWidget.getRootWidget();
    }

    /**
     * Validates a destination before it modifies a collection, replacement slot or update queue.
     *
     * @param destination destination container widget
     */
    final void validateAttachment(final Widget<?> destination) {
        if (this instanceof RootWidget || this.parent == destination) {
            throw new IllegalArgumentException("A root or duplicate child cannot be attached");
        }
        for (Widget<?> ancestor = destination; ancestor != null;
                ancestor = ancestor.parent instanceof Widget<?> node ? node : null) {
            if (ancestor == this) {
                throw new IllegalArgumentException("A widget cannot contain itself or an ancestor");
            }
            if (ancestor.retired) {
                throw new IllegalArgumentException("The destination has been disposed");
            }
        }
        this.validateSubtreeOwner(destination.attachmentRootId());
    }

    /**
     * Returns the actual or previously acquired root identity for accepting children.
     *
     * @return root identity, or null for an unused container
     */
    private RMId attachmentRootId() {
        return this.getRootWidget().map(RootWidget::getId).orElse(this.ownerRootId);
    }

    /**
     * Before batch construction, validates all arguments without changing their current parents.
     *
     * @param children children in constructor order
     */
    final void validateChildren(final Widget<?>[] children) {
        final Set<RMId> seen = new HashSet<>();
        for (final Widget<?> child : children) {
            Objects.requireNonNull(child, "widget").validateAttachment(this);
            if (!seen.add(child.getId())) {
                throw new IllegalArgumentException("Duplicate child in constructor");
            }
        }
    }

    /**
     * Checks that this subtree is available for the specified browser runtime.
     *
     * @param rootId required root identity, or null if not yet known
     */
    private void validateSubtreeOwner(final RMId rootId) {
        this.visitSubtree(widget -> {
            if (widget.retired || rootId != null
                    && widget.ownerRootId != null && !rootId.equals(widget.ownerRootId)) {
                throw new IllegalArgumentException("A disposed or foreign-root widget cannot move");
            }
        });
    }

    /**
     * Adds the destination update before the former owner removes the child. A same-root
     * browser first moves the existing element and then ignores removal from its old host.
     * A detached subtree's buffered updates are refreshed before attachment.
     *
     * @param destination destination container widget
     * @param attachment destination attachment update factory
     * @param <C> structural destination widget type
     */
    final <C extends Widget<?> & Container> void attachTo(final C destination,
            final Supplier<Update> attachment) {
        final Container previous = this.parent;
        final Optional<RootWidget> oldRoot = this.getRootWidget();
        if (oldRoot.isEmpty() || !oldRoot.equals(destination.getRootWidget())) {
            final Map<Update, Widget<?>> pending = new TreeMap<>();
            this.visitSubtree(widget -> {
                final Set<Update> updates = new TreeSet<>();
                widget.getUpdates(updates);
                updates.forEach(update -> pending.put(update, widget));
            });
            pending.forEach((update, widget) -> widget.pushUpdate(update.clone()));
        }
        destination.pushUpdate(attachment.get());
        if (previous != null) {
            previous.remove(this);
        }
        this.parent = destination;
        this.getRootWidget().ifPresent(root -> this.visitSubtree(root::trackOwned));
    }

    /**
     * Records the runtime to which the first widget creation is published.
     *
     * @param owner browser root identity
     */
    final void markExported(final RMId owner) {
        this.ownerRootId = owner;
    }

    /**
     * Clears the backlink after the current owner has removed the child from its storage.
     *
     * @param owner container doing the removal
     */
    final void detachFrom(final Container owner) {
        if (this.parent == owner) {
            this.parent = null;
            this.visitSubtree(widget -> widget.setBufferOwner(null));
        }
    }

    /**
     * Visits this widget and its structural descendants exactly once.
     *
     * @param visitor operation to apply
     */
    private void visitSubtree(final Consumer<Widget<?>> visitor) {
        if (this instanceof Container container) {
            for (final Widget<?> widget : container) {
                visitor.accept(widget);
            }
        } else {
            visitor.accept(this);
        }
    }

    /**
     * Sets the widget style by replacing models inside bindings or creating new bindings.
     *
     * @param style new widget style
     */
    public void setStyle(final S style) {
        this.requireUsable();
        this.style = style;
        style.forEachModel((state, property, model) -> {
            final Map<Property<?>, Binding<?>> subset =
                this.bindings.computeIfAbsent(state, s -> new HashMap<>());
            final Binding<?> binding = subset.get(property);
            if (binding == null) {
                subset.put(property, property.bindStyleModel(state, model, this));
            } else {
                property.rebindStyleModel(binding, model);
            }
        });
    }

    /**
     * Adds an update for this widget.
     *
     * @param update the update to add
     */
    protected synchronized void pushUpdate(final Update update) {
        if (this.retired || this.bufferOverflow) {
            return;
        }
        final RootWidget owner = this.bufferOwner == null ? null : this.bufferOwner.get();
        final long bytes = update.encodedBytes();
        final int count = this.updates == null ? 0 : this.updates.size();
        if (owner != null ? !owner.retainBuffered(1, bytes)
                : count >= DETACHED_LIMITS.pendingUpdates()
                    || this.bufferedBytes + bytes > DETACHED_LIMITS.pendingBytes()) {
            this.releaseBudget();
            this.bufferOverflow = true;
            this.updates = null;
            this.bufferedBytes = 0;
            return;
        }
        if (this.updates == null) {
            this.updates = new ArrayList<>();
        }
        this.updates.add(update);
        this.bufferedBytes += bytes;
    }

    /**
     * Returns a typed {@link Binding} for the given {@link State} and {@link Property},
     * automatically creating it if missing.
     *
     * @param state the widget’s current logical state (e.g. NORMAL, HOVERED)
     * @param property the property key (e.g. TEXT, COLOR)
     * @param <T> the type parameter of the model’s data
     * @return an existing or newly created {@link Binding} for the given property and state
     * @throws IllegalArgumentException if the existing binding has an incompatible model type
     */
    private synchronized <T> Binding<T> getBinding(
            final State state, final Property<T> property) {
        this.requireUsable();
        Map<Property<?>, Binding<?>> subset
            = this.bindings.computeIfAbsent(state, k -> new HashMap<>());
        final Binding<?> existingBinding = subset.get(property);
        if (existingBinding == null) {
            final Model<T> defaultModel = property.createDefaultModel();
            final Binding<T> newBinding = property.bindModel(state, defaultModel, this);
            subset.put(property, newBinding);
            return newBinding;
        } else {
            return property.cast(existingBinding);
        }
    }

    /**
     * A generic listener that reacts to model value changes
     * and automatically sends a corresponding {@link SetProperty} update to the client.
     *
     * @param <T> the type of the data produced by the model
     */
    public static class PropertyListener<T> implements Listener<T> {
        /**
         * The widget that owns this listener and receives updates.
         */
        private final Widget<?> widget;

        /**
         * The logical widget state to which this listener applies.
         */
        private final State state;

        /**
         * The property associated with the observed model.
         */
        private final Property<T> property;

        /**
         * Creates a new property listener that forwards model updates
         * to the specified widget.
         *
         * @param widget the target widget that should receive updates
         * @param state the logical state of the widget
         * @param property the property that this listener represents
         */
        public PropertyListener(final Widget<?> widget, final State state,
                final Property<T> property) {
            this.widget = widget;
            this.state = state;
            this.property = property;
        }

        @Override
        public void accept(final T data) {
            this.widget.pushUpdate(
                new SetProperty<>(this.widget.getId(), this.state, this.property, data)
            );
        }
    }
}
