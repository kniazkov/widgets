/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.protocol.Update;
import java.util.Collection;
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
    private final NavigableMap<Long, Update> pending = new TreeMap<>();

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
        if (sequence > this.published) {
            return false;
        }
        if (sequence > this.acknowledged) {
            this.pending.headMap(sequence, true).clear();
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
        for (final Update update : updates) {
            if (this.published == MAX_SEQUENCE) {
                throw new IllegalStateException("Widget update stream exhausted");
            }
            this.pending.put(++this.published, update);
        }
    }

    /**
     * Serializes the unacknowledged suffix. A fresh runtime's first prefix is its snapshot.
     *
     * @param response browser-visible response object
     */
    void serialize(final JsonObject response) {
        response.addString("updateEpoch", this.epoch);
        response.addNumber("updateBase", this.acknowledged);
        response.addNumber("updateThrough", this.published);
        response.addBoolean("snapshot", this.acknowledged == 0);
        final JsonArray updates = response.createArray("updates");
        this.pending.forEach((sequence, update) -> {
            final JsonObject object = updates.createObject();
            update.serialize(object);
            object.addNumber("sequence", sequence);
        });
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
