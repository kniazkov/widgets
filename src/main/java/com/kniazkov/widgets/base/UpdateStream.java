/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.protocol.Update;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.logging.Logger;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Ordered outgoing updates for one browser runtime. Methods are called under the page lock.
 */
final class UpdateStream {
    /**
     * Version of the update envelope, independent of transport authentication.
     */
    static final String VERSION = "2";

    /**
     * Mandatory capability negotiated at runtime creation.
     */
    static final String CAPABILITY = "sequenced-updates";

    /**
     * Highest sequence exactly representable as a browser integer.
     */
    private static final long MAX_SEQUENCE = 9_007_199_254_740_991L;

    /**
     * Public generation identifier, not an authentication credential or global widget ID.
     */
    private final String epoch = UUID.randomUUID().toString();

    /**
     * Published but unacknowledged updates, keyed by stable channel sequence.
     */
    private final NavigableMap<Long, Entry> pending = new TreeMap<>();

    /**
     * Bounded response and retention policy.
     */
    private final ProtocolLimits limits;

    /**
     * Monotonic clock shared by retention checks.
     */
    private final LongSupplier clock;

    /**
     * Aggregate diagnostics contain no widget names or payload values.
     */
    private static final Logger LOGGER = Logger.getLogger(UpdateStream.class.getName());

    /**
     * Exact retained object JSON bytes with bounded per-entry sequence framing.
     */
    private long pendingBytes;

    /**
     * Number of updates resent in the same epoch.
     */
    private long replays;

    /**
     * Largest sequence actually supplied in a batch, distinct from the enqueued sequence.
     */
    private long sentThrough;

    /**
     * Terminal overflow or expiration.
     */
    private boolean resync;

    /**
     * Uses default budgets for ordinary in-process use.
     */
    UpdateStream() {
        this(ProtocolLimits.defaults(), System::nanoTime);
    }

    /**
     * @param limits configured budgets
     * @param clock monotonic time source
     */
    UpdateStream(final ProtocolLimits limits, final LongSupplier clock) {
        this.limits = limits;
        this.clock = clock;
    }

    /**
     * Highest published checkpoint.
     */
    private long published;

    /**
     * Highest accepted client acknowledgement.
     */
    private long acknowledged;

    /**
     * Returns the unique epoch of this runtime.
     *
     * @return epoch identifier
     */
    String epoch() {
        return this.epoch;
    }

    /**
     * Accepts only an emitted checkpoint of this epoch. Older retries never rewind the stream.
     *
     * @param requestedEpoch epoch attached by the browser
     * @param checkpoint last successfully handled update
     * @param required whether the request crosses the authenticated HTTP boundary
     * @return whether this request can proceed
     */
    boolean acknowledge(final String requestedEpoch, final String checkpoint,
            final boolean required) {
        if ((requestedEpoch != null || required) && !this.epoch.equals(requestedEpoch)) {
            return false;
        }
        if (checkpoint == null) {
            return true;
        }
        if (!checkpoint.matches("#(?:0|[1-9][0-9]{0,15})")) {
            return false;
        }
        final long sequence = Long.parseLong(checkpoint.substring(1));
        if (sequence > this.sentThrough) {
            return false;
        }
        if (sequence > this.acknowledged) {
            final var released = this.pending.headMap(sequence, true);
            released.values().forEach(entry -> this.pendingBytes -= entry.bytes);
            released.clear();
            this.acknowledged = sequence;
        }
        return true;
    }

    /**
     * Publishes pending model updates in the supplied order without inferring RMId continuity.
     *
     * @param updates model updates ready for this browser
     */
    void publish(final Collection<Update> updates) {
        if (this.requiresResync()) {
            return;
        }
        final long now = this.clock.getAsLong();
        for (final Update update : updates) {
            final Object key = update.coalescingKey();
            final var last = this.pending.lastEntry();
            if (key != null && last != null && last.getKey() > this.sentThrough
                    && Objects.equals(key, last.getValue().key)) {
                this.pendingBytes -= this.pending.pollLastEntry().getValue().bytes;
                this.published--;
            }
            final JsonObject wire = new JsonObject();
            update.serialize(wire);
            wire.addNumber("sequence", this.published + 1);
            final long bytes = wire.toString().getBytes(StandardCharsets.UTF_8).length + 1L;
            if (this.published == MAX_SEQUENCE || bytes > this.limits.updateBytesPerBatch()
                    || this.pending.size() >= this.limits.pendingUpdates()
                    || this.pendingBytes + bytes > this.limits.pendingBytes()) {
                this.requestResync();
                return;
            }
            this.pending.put(++this.published, new Entry(wire, key, bytes, now));
            this.pendingBytes += bytes;
        }
    }

    /**
     * @return whether this runtime requires a fresh snapshot
     */
    boolean requiresResync() {
        final var first = this.pending.firstEntry();
        if (first != null && this.clock.getAsLong() - first.getValue().created
                >= TimeUnit.MILLISECONDS.toNanos(this.limits.updateLifetimeMillis())) {
            this.requestResync();
        }
        return this.resync;
    }

    /**
     * Ends replay for this epoch. The next page instance gets a new epoch and snapshot.
     */
    void requestResync() {
        if (!this.resync) {
            LOGGER.info("Update queue resync; depth=" + this.pending.size()
                + "; bytes=" + this.pendingBytes + "; replays=" + this.replays);
            this.resync = true;
            this.pending.clear();
            this.pendingBytes = 0;
        }
    }

    /**
     * Serializes only one bounded prefix; each batch retains the current epoch.
     *
     * @param response browser-visible response object
     */
    void serialize(final JsonObject response) {
        if (this.requiresResync()) {
            response.addBoolean("updateResyncRequired", true);
            response.addBoolean("snapshotUnavailable", this.sentThrough == 0);
            return;
        }
        response.addString("updateEpoch", this.epoch);
        response.addNumber("updateBase", this.acknowledged);
        response.addBoolean("snapshot", this.acknowledged == 0);
        final JsonArray updates = response.createArray("updates");
        long through = this.acknowledged;
        long bytes = 0;
        for (final var item : this.pending.entrySet()) {
            final Entry entry = item.getValue();
            if (through - this.acknowledged >= this.limits.updatesPerBatch()
                    || bytes + entry.bytes > this.limits.updateBytesPerBatch()) {
                break;
            }
            updates.addElement(entry.wire);
            through = item.getKey();
            if (through <= this.sentThrough) {
                this.replays++;
            }
            bytes += entry.bytes;
        }
        this.sentThrough = Math.max(this.sentThrough, through);
        response.addNumber("updateThrough", through);
        response.addBoolean("moreUpdates", through < this.published);
    }

    /**
     * Retained update and its budget charge.
     *
     * @param wire published representation
     * @param key identity if eligible to coalesce before sending
     * @param bytes encoded budget charge
     * @param created monotonic arrival time
     */
    private record Entry(JsonObject wire, Object key, long bytes, long created) {
    }

    /**
     * Signals incompatibility without including data to apply or consume.
     *
     * @return terminal protocol response
     */
    static JsonObject mismatch() {
        final JsonObject response = new JsonObject();
        markMismatch(response);
        return response;
    }

    /**
     * Signals a failed update handshake or acknowledgement in an existing response.
     *
     * @param response outgoing response
     */
    static void markMismatch(final JsonObject response) {
        response.addBoolean("protocolMismatch", true);
        response.addBoolean("updateProtocolMismatch", true);
    }
}
