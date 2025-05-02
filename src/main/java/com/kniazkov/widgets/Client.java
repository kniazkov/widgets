/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a single connected client in the system.
 * <p>
 *     An instance of this class corresponds to one active web page or browser tab.
 *     Each client is uniquely identified by a {@link UId} and contains its own widget tree,
 *     managed by a {@link RootWidget}. The client tracks all registered widgets, handles
 *     incoming events from the front end, and collects outgoing updates for transmission.
 * </p>
 */
public final class Client implements Comparable<Client> {
    /**
     * Unique identifier for this client instance.
     */
    private final UId id;

    /**
     * Client expiration timer in milliseconds.
     * <p>
     *     When this value reaches zero, the client is considered expired and should be removed.
     *     It is the responsibility of external code (e.g., a scheduler) to decrement this timer
     *     and act accordingly.
     * </p>
     */
    long timer;

    /**
     * Mapping of all widgets associated with this client, indexed by their unique identifiers.
     * <p>
     *     This collection serves as a registry for event routing and state updates.
     *     Widgets are stored in a sorted map for predictable iteration order.
     * </p>
     */
    final Map<UId, Widget> widgets;

    /**
     * The root widget of the client, representing the entry point to the widget hierarchy.
     */
    private final RootWidget root;

    /**
     * Constructs a new client with a unique ID and an empty widget registry.
     * <p>
     *     A new {@link RootWidget} is created and associated with this client.
     * </p>
     */
    Client() {
        this.id = UId.create();
        this.widgets = new TreeMap<>();
        this.root = new RootWidget(this);
    }

    /**
     * Returns the unique identifier assigned to this client.
     *
     * @return Client ID
     */
    UId getId() {
        return this.id;
    }

    /**
     * Returns the root widget of the client.
     *
     * @return The root widget
     */
    RootWidget getRootWidget() {
        return this.root;
    }

    /**
     * Collects update instructions from all widgets in this client.
     * <p>
     *     These instructions represent state changes to be pushed to the front end.
     *     The result is a sorted list of unique instructions (duplicates removed).
     * </p>
     *
     * @return Sorted list of instructions to be sent to the client
     */
    List<Instruction> collectUpdates() {
        final Set<Instruction> set = new TreeSet<>();
        for (final Widget widget : widgets.values()) {
            widget.getUpdates(set);
        }
        return new ArrayList<>(set);
    }

    /**
     * Dispatches an event received from the front end to the appropriate widget.
     * <p>
     *     If no matching widget is found for the given ID, the event is ignored.
     * </p>
     *
     * @param widgetId The identifier of the widget that the event targets
     * @param type The type of the event (e.g., "click", "input")
     * @param data Optional JSON payload associated with the event
     */
    void handleEvent(final UId widgetId, final String type, final Optional<JsonObject> data) {
        final Widget widget = this.widgets.get(widgetId);
        if (widget != null) {
            widget.handleEvent(type, data);
        }
    }

    /**
     * Cleans up client state before destruction.
     * <p>
     *     Removes all widgets and performs any additional teardown logic.
     *     This method should be called when the client session expires or is explicitly terminated.
     * </p>
     */
    void destroy() {
        this.widgets.clear();
        //...
    }

    /**
     * Compares this client to another by their unique ID.
     */
    @Override
    public int compareTo(final Client other) {
        return this.id.compareTo(other.id);
    }

    /**
     * Returns {@code true} if this client has the same ID as another.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Client) {
            final Client other = (Client) obj;
            return this.id.equals(other.id);
        }
        return false;
    }

    /**
     * Returns hash code based on client ID.
     */
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
