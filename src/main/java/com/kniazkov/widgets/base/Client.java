/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonException;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;
import com.kniazkov.widgets.protocol.Update;
import com.kniazkov.widgets.view.RootWidget;
import com.kniazkov.widgets.view.Widget;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Represents a single connected client in the system.
 * An instance of this class corresponds to one active web page or browser tab.
 * Each client is uniquely identified by a {@link UId} and contains its own widget tree,
 * managed by a {@link RootWidget}. The client tracks all registered widgets, handles
 * incoming events from the front end, and collects outgoing updates for transmission.
 */
public final class Client implements Comparable<Client> {
    /**
     * Unique identifier for this client instance.
     */
    private final UId id;

    /**
     * Client expiration timer in milliseconds.
     * When this value reaches zero, the client is considered expired and should be removed.
     * It is the responsibility of external code (e.g., a scheduler) to decrement this timer
     * and act accordingly.
     */
    long timer;

    /**
     * The root widget of the client, representing the entry point to the widget hierarchy.
     */
    private final RootWidget root;

    /**
     * Set of updates collected from widgets but not processed by the client.
     */
    private final Set<Update> updates;

    /**
     * The identifier of the last event processed.
     */
    private UId lastHandledEventId = UId.INVALID;

    /**
     * Constructs a new client with a unique ID and an empty widget registry.
     * A new {@link RootWidget} is created and associated with this client.
     */
    Client() {
        this.id = UId.create();
        this.root = new RootWidget();
        this.updates = new TreeSet<>();
    }

    /**
     * Returns the unique identifier assigned to this client.
     *
     * @return client ID
     */
    UId getId() {
        return this.id;
    }

    /**
     * Returns the root widget of the client.
     *
     * @return the root widget
     */
    RootWidget getRootWidget() {
        return this.root;
    }

    /**
     * Processes a synchronization request from the client.
     * <p>
     *     This method handles two main tasks:
     * </p>
     * <ul>
     *     <li>
     *         Processes the list of incoming events (if present) and dispatches them
     *         to the corresponding widgets using {@link Widget#handleEvent(String, Optional)}.
     *         Events that have already been handled (based on {@code lastHandledEventId})
     *         are skipped.
     *     </li>
     *     <li>
     *         Removes already processed update instructions (based on {@code lastUpdate})
     *         and serializes the remaining ones into the {@code updates} array in the response.
     *     </li>
     * </ul>
     *
     * @param request  A map containing client parameters, including events and acknowledged
     *  instructions
     * @param response A JSON object that will be filled with pending updates and the last
     *  processed event ID
     */
    void synchronize(final Map<String, String> request, final JsonObject response) {
        final List<Widget> widgets = this.root.collectAllWidgets();
        this.processEvents(request, widgets);
        this.collectUpdates(request, widgets);
        this.serializeUpdates(response);
    }

    /**
     * Processes incoming events from the client and dispatches them to appropriate widgets.
     *
     * @param request The map of client parameters containing the "events" key
     * @param widgets List of widgets collected from the widget tree
     */
    private void processEvents(final Map<String, String> request, final List<Widget> widgets) {
        if (!request.containsKey("events")) {
            return;
        }
        try {
            final JsonElement element = Json.parse(request.get("events"));
            final JsonArray events = element.toJsonArray();
            if (events == null || events.isEmpty()) {
                return;
            }
            final Map<UId, Widget> map = new TreeMap<>();
            for (final Widget widget : widgets) {
                map.put(widget.getId(), widget);
            }
            for (JsonElement item : events) {
                final JsonObject event = item.toJsonObject();
                if (event == null) continue;

                final UId eventId = UId.parse(event.get("id").getStringValue());
                if (eventId.compareTo(this.lastHandledEventId) <= 0) continue;

                final UId widgetId = UId.parse(event.get("widget").getStringValue());
                final Widget widget = map.get(widgetId);
                if (widget == null) continue;

                final String type = event.get("type").getStringValue();
                final Optional<JsonObject> data = event.containsKey("data")
                    ? Optional.ofNullable(event.get("data").toJsonObject())
                    : Optional.empty();

                widget.handleEvent(type, data);
                this.lastHandledEventId = eventId;
            }
        } catch (final JsonException ignored) {
        }
    }

    /**
     * Collects updates from widgets, adding them to the set.
     * Removes from the set any updates that have already been processed by the client.
     *
     * @param request The map of client parameters containing the "lastUpdate" key
     * @param widgets List of widgets collected from the widget tree
     */
    private void collectUpdates(final Map<String, String> request, final List<Widget> widgets) {
        if (request.containsKey("lastUpdate")) {
            final UId id = UId.parse(request.get("lastUpdate"));
            this.updates.removeIf(update -> update.getId().compareTo(id) <= 0);
        }
        for (final Widget widget : widgets) {
            widget.getUpdates(this.updates);
        }
    }

    /**
     * Serializes the list of remaining updates into the JSON response.
     *
     * @param response The response JSON object to populate
     */
    private void serializeUpdates(final JsonObject response) {
        response.addString("lastEvent", this.lastHandledEventId.toString());
        final JsonArray updates = response.createArray("updates");
        for (final Update update : this.updates) {
            update.serialize(updates.createObject());
        }
    }

    /**
     * Cleans up client state before destruction.
     */
    void destroy() {
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
