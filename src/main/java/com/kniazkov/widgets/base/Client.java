/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonParsingMode;
import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonException;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;
import com.kniazkov.widgets.common.UploadProtocol;
import com.kniazkov.widgets.protocol.Update;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.RootWidget;
import com.kniazkov.widgets.view.Widget;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents a single connected client in the system.
 * An instance of this class corresponds to one active web page or browser tab.
 * Each client is uniquely identified by a {@link RMId} and contains its own widget tree,
 * managed by a {@link RootWidget}. The client tracks all registered widgets, handles
 * incoming events from the front end, and collects outgoing updates for transmission.
 */
public final class Client implements Comparable<Client>, AutoCloseable {
    /**
     * Shared revocable document identity.
     */
    private final TabContext tabContext;

    /**
     * Unique identifier for this client instance.
     */
    private final RMId id;

    /**
     * Independent 256-bit secret for this page, never a widget ID or browser ID.
     */
    private final String channelToken = ProtocolSecurity.secret();

    /**
     * Client expiration timer in milliseconds.
     * When this value reaches zero, the client is considered expired and should be removed.
     * It is the responsibility of external code (e.g., a scheduler) to decrement this timer
     * and act accordingly.
     */
    long timer;

    /**
     * Whether this client has already submitted its one diagnostic report.
     */
    boolean errorReported;

    /**
     * The root widget of the client, representing the entry point to the widget hierarchy.
     */
    private final RootWidget root;

    /**
     * Outgoing independently sequenced browser channel.
     */
    private final UpdateStream updates = new UpdateStream();

    /**
     * Scratch set keeps model update order while each widget transfers its own queue.
     */
    private final Set<Update> collecting = new TreeSet<>();

    /**
     * The identifier of the last event processed.
     */
    private RMId lastHandledEventId = RMId.INVALID;

    /**
     * Constructs a new client with a unique ID and an empty widget registry.
     * A new {@link RootWidget} is created and associated with this client.
     */
    Client() {
        this(new Options.Builder().build());
    }

    /**
     * Constructs a new client using the supplied immutable application options.
     *
     * @param options application options shared by the client widget tree
     */
    Client(final Options options) {
        this(options, new TabContext(java.util.UUID.randomUUID(), Long.MAX_VALUE));
    }

    /**
     * Configures upload resource ownership.
     * @param options application options
     * @param tabContext shared document context
     */
    Client(final Options options, final TabContext tabContext) {
        this(options, tabContext, new com.kniazkov.widgets.view.UploadRegistry(options));
    }

    /**
     * Configures upload resource ownership.
     * @param options application configuration
     * @param tabContext document context
     * @param uploads application-wide upload resources
     */
    Client(final Options options, final TabContext tabContext,
            final com.kniazkov.widgets.view.UploadRegistry uploads) {
        this.tabContext = tabContext;
        this.id = RMId.create();
        this.root = new RootWidget(options, uploads);
    }

    /**
     * Returns revocable document context.
     * @return revocable document context
     */
    TabContext getTabContext() {
        return this.tabContext;
    }

    /**
     * Returns channel secret, only for authenticated creation and verification.
     * @return channel secret, only for authenticated creation and verification
     */
    String channelToken() {
        return this.channelToken;
    }

    /**
     * Returns the sequence epoch independently of the server instance and channel token.
     *
     * @return outgoing stream epoch
     */
    String updateEpoch() {
        return this.updates.epoch();
    }

    /**
     * Returns the unique identifier assigned to this client.
     *
     * @return client ID
     */
    RMId getId() {
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
     *         to the corresponding widgets using {@link Widget#handleEvent(String, JsonObject)}.
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
        this.synchronize(request, response, false);
    }

    /**
     * Processes synchronization with optional mandatory stream credentials.
     *
     * @param request browser request
     * @param response target response
     * @param requireEpoch whether this is an authenticated HTTP request
     */
    void synchronize(final Map<String, String> request, final JsonObject response,
            final boolean requireEpoch) {
        synchronized (this.root) {
            this.requireLiveSession();
            if (!this.tabContext.isActive()) {
                this.root.closeUploads();
                response.addBoolean("tabContextExpired", true);
                return;
            }
            if (!this.updates.acknowledge(request.get("updateEpoch"), request.get("lastUpdate"),
                    requireEpoch)) {
                UpdateStream.markMismatch(response);
                return;
            }
            this.processEvents(request, response);
            this.requireLiveSession();
            if (!this.tabContext.isActive()) {
                this.root.closeUploads();
                response.addBoolean("tabContextExpired", true);
                return;
            }
            this.collectUpdates();
            this.serializeUpdates(response);
        }
    }

    /**
     * Delivers one binary upload chunk to a file-loader widget.
     *
     * @param widgetId target file-loader identifier
     * @param fileId browser-local file identifier
     * @param chunkIndex zero-based chunk index
     * @param data binary chunk content
     * @param lastUpdate last browser-applied widget update
     * @return serialized upload acknowledgement
     */
    JsonObject uploadChunk(
            final RMId widgetId,
            final int fileId,
            final int chunkIndex,
            final byte[] data,
            final String lastUpdate) {
        return this.uploadChunk(widgetId, fileId, chunkIndex, data, lastUpdate, null, false);
    }

    /**
     * Delivers one chunk with the browser's outgoing-update acknowledgement.
     *
     * @param widgetId upload target
     * @param fileId browser-local file identifier
     * @param chunkIndex chunk index
     * @param data chunk bytes
     * @param lastUpdate last applied sequence
     * @param epoch outgoing stream generation
     * @param requireEpoch whether this is an authenticated HTTP request
     * @return upload receipt or protocol failure
     */
    JsonObject uploadChunk(final RMId widgetId, final int fileId, final int chunkIndex,
            final byte[] data, final String lastUpdate, final String epoch,
            final boolean requireEpoch) {
        synchronized (this.root) {
            this.requireLiveSession();
            if (!this.tabContext.isActive()) {
                this.root.closeUploads();
                return UploadProtocol.rejected();
            }
            if (!this.updates.acknowledge(epoch, lastUpdate, requireEpoch)) {
                return UpdateStream.mismatch();
            }
            for (final Widget<?> widget : this.root) {
                if (widget.getId().equals(widgetId) && widget instanceof FileLoader) {
                    final JsonObject response = ((FileLoader) widget).handleUploadChunk(
                        fileId,
                        chunkIndex,
                        data
                    );
                    if (response == UploadProtocol.rejected()) {
                        return response;
                    }
                    this.requireLiveSession();
                    if (!this.tabContext.isActive()) {
                        this.root.closeUploads();
                        return UploadProtocol.rejected();
                    }
                    this.collectUpdates();
                    this.serializeUpdates(response);
                    return response;
                }
            }
        }
        return UploadProtocol.rejected();
    }

    /**
     * Rechecks revocation after waiting for the root and after application callbacks.
     */
    private void requireLiveSession() {
        if (this.closed) {
            throw new ProtocolAccessException(true);
        }
        final BrowserSession owner = this.tabContext.getBrowserSession();
        if (owner != null && !owner.isActive()) {
            this.root.closeUploads();
            throw new ProtocolAccessException(true);
        }
    }

    /**
     * Processes incoming events from the client and dispatches them to appropriate widgets.
     *
     * @param request the map of client parameters containing the "events" key
     */
    private void processEvents(final Map<String, String> request, final JsonObject response) {
        if (!request.containsKey("events")) {
            return;
        }
        try {
            final JsonElement element = Json.parse(request.get("events"), JsonParsingMode.STRICT);
            final JsonArray events = element.toJsonArray().orElse(null);
            if (events == null || !validEventSequence(events)) {
                response.addBoolean("eventResyncRequired", true);
                return;
            }
            for (final JsonElement item : events) {
                this.requireLiveSession();
                if (!this.tabContext.isActive()) {
                    this.root.closeUploads();
                    return;
                }
                this.processEvent(item.objectValueOrThrow(), response);
            }

        } catch (final JsonException ignored) {
            response.addBoolean("eventResyncRequired", true);
        }
    }

    /**
     * Resolves each target in the current tree, after all preceding callbacks have finished.
     * No iterator or index survives a callback that can mutate the tree.
     */
    private void processEvent(final JsonObject event, final JsonObject response) {
        final RMId widgetId = getEventWidgetId(event);
        Widget<?> widget = null;
        for (final Widget<?> child : this.root) {
            if (child.getId().equals(widgetId)) {
                widget = child;
                break;
            }
        }
        handleEventObject(event, widget, response);
    }

    /**
     * Dispatches an event to its target widget.
     */
    private void handleEventObject(final JsonObject event, final Widget<?> widget,
            final JsonObject response) {
        if (!this.tabContext.isActive()) {
            this.root.closeUploads();
            return;
        }
        this.requireLiveSession();
        final String serializedId = event.get("id").stringValue();
        final RMId eventId = RMId.parse(serializedId);
        if (eventId.compareTo(this.lastHandledEventId) <= 0) {
            eventResult(response, serializedId, "obsolete", "alreadyConsumed");
            return;
        }
        final String status;
        final String reason;
        if (!event.get("type").isString()
                || getEventWidgetId(event).equals(RMId.INVALID)) {
            status = "rejected";
            reason = "invalidEnvelope";
        } else if (widget == null || widget.getRootWidget().orElse(null) != this.root) {
            status = "obsolete";
            reason = "detachedWidget";
        } else {
            final JsonObject data = event.containsKey("data")
                ? event.get("data").toJsonObject().orElse(null) : new JsonObject();
            final com.kniazkov.widgets.controller.Event<?> kind =
                com.kniazkov.widgets.controller.Event.getByName(event.get("type").stringValue());
            final java.util.Optional<String> rejection = kind.rejectionReason(widget, data);
            if (rejection.isEmpty()) {
                /*
                 * Consume before invoking application code: an exception after a side effect
                 * must not cause that side effect to be repeated on a lost-response retry.
                 */
                this.lastHandledEventId = eventId;
                eventResult(response, serializedId, "applied", "");
                widget.handleEvent(kind, data);
                return;
            }
            status = "rejected";
            reason = rejection.get();
        }
        this.lastHandledEventId = eventId;
        eventResult(response, serializedId, status, reason);
    }

    /**
     * Preflights identifiers before dispatch so a corrupt/out-of-order batch cannot skip
     * an earlier action through the cumulative ACK. Duplicate IDs are safe and permitted.
     */
    private static boolean validEventSequence(final JsonArray events) {
        RMId previous = RMId.INVALID;
        for (final JsonElement item : events) {
            final JsonObject event = item.toJsonObject().orElse(null);
            if (event == null || !event.get("id").isString()) {
                return false;
            }
            final String value = event.get("id").stringValue();
            final RMId id = RMId.parse(value);
            if (id.equals(RMId.INVALID) || !id.toString().equals(value)
                    || id.compareTo(previous) < 0) {
                return false;
            }
            previous = id;
        }
        return true;
    }

    /**
     * Adds a terminal delivery result without retaining an unbounded per-client history.
     * The R02 rejectedEvents projection remains available to existing integrations.
     */
    private static void eventResult(final JsonObject response, final String id,
            final String status, final String reason) {
        final JsonArray results = response.containsKey("eventResults")
            ? response.get("eventResults").arrayValueOrThrow()
            : response.createArray("eventResults");
        final JsonObject result = results.createObject();
        result.addString("id", id);
        result.addString("status", status);
        if (!reason.isEmpty()) {
            result.addString("reason", reason);
        }
        if ("rejected".equals(status) || "detachedWidget".equals(reason)) {
            final JsonArray rejected = response.containsKey("rejectedEvents")
                ? response.get("rejectedEvents").arrayValueOrThrow()
                : response.createArray("rejectedEvents");
            final JsonObject item = rejected.createObject();
            item.addString("id", id);
            item.addString("reason", reason);
        }
    }

    /**
     * Returns the target widget identifier, or {@link RMId#INVALID} for malformed events.
     */
    private static RMId getEventWidgetId(final JsonObject event) {
        if (!event.containsKey("widget") || !event.get("widget").isString()) {
            return RMId.INVALID;
        }
        final String serializedId = event.get("widget").stringValue();
        return RMId.parse(serializedId);
    }

    /**
     * Collects new model updates in creation order and assigns stable channel sequences.
     */
    private void collectUpdates() {
        this.root.collectBrowserUpdates(this.collecting);
        this.updates.publish(this.collecting);
        this.collecting.clear();
    }

    /**
     * Serializes the list of remaining updates into the JSON response.
     *
     * @param response The response JSON object to populate
     */
    private void serializeUpdates(final JsonObject response) {
        response.addString("lastEvent", this.lastHandledEventId.toString());
        this.updates.serialize(response);
    }

    /**
     * Terminal page lifecycle, guarded by the root monitor.
     */
    private boolean closed;

    /**
     * Releases this page without revoking sibling pages in the same document.
     */
    @Override
    public void close() {
        synchronized (this.root) {
            if (this.closed) {
                return;
            }
            this.closed = true;
        }
        this.root.closePage();
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
