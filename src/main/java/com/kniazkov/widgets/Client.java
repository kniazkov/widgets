/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonException;
import com.kniazkov.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
     * A list of update instructions that should be sent to the client.
     */
    private final List<Instruction> updates;

    /**
     * The root widget of the client, representing the entry point to the widget hierarchy.
     */
    private final RootWidget root;

    /**
     * The identifier of the last event processed.
     */
    private UId lastHandledEventId = UId.INVALID;

    /**
     * Constructs a new client with a unique ID and an empty widget registry.
     * <p>
     *     A new {@link RootWidget} is created and associated with this client.
     * </p>
     */
    Client() {
        this.id = UId.create();
        this.widgets = new TreeMap<>();
        this.updates = new ArrayList<>();
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
     * Queues an instruction to be sent to the client during the next update cycle.
     *
     * @param instruction The instruction to add
     */
    void addInstruction(final Instruction instruction) {
        this.updates.add(instruction);
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
     *         Removes already acknowledged update instructions (based on {@code lastInstruction})
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
        if (request.containsKey("events")) {
            try {
                final JsonElement element = Json.parse(request.get("events"));
                final JsonArray events = element.toJsonArray();
                if (events != null) {
                    for (JsonElement item : events) {
                        final JsonObject event = item.toJsonObject();
                        if (event == null) {
                            continue;
                        }
                        final UId eventId = UId.parse(event.get("id").getStringValue());
                        if (eventId.compareTo(this.lastHandledEventId) <= 0) {
                            continue;
                        }
                        final UId widgetId = UId.parse(event.get("widget").getStringValue());
                        final Widget widget = this.widgets.get(widgetId);
                        if (widget == null) {
                            continue;
                        }
                        final String type = event.get("type").getStringValue();
                        final Optional<JsonObject> data;
                        if (event.containsKey("data")) {
                            data = Optional.ofNullable(event.get("data").toJsonObject());
                        } else {
                            data = Optional.empty();
                        }
                        widget.handleEvent(type, data);
                        this.lastHandledEventId = eventId;
                    }
                }
            } catch (final JsonException ignored) {
            }
        }
        response.addString("lastEvent", this.lastHandledEventId.toString());

        if (request.containsKey("lastInstruction")) {
            final UId id = UId.parse(request.get("lastInstruction"));
            this.updates.removeIf(instr -> instr.getInstrId().compareTo(id) <= 0);
        }

        final JsonArray updates = response.createArray("updates");
        for (final Instruction instruction : this.updates) {
            instruction.serialize(updates.createObject());
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
