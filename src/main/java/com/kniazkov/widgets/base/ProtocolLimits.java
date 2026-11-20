/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

/**
 * Bounded event requests, buffered updates and replay batches per page runtime.
 *
 * @param pendingUpdates maximum unacknowledged updates
 * @param pendingBytes maximum JSON bytes held for replay
 * @param updatesPerBatch maximum updates in one response
 * @param updateBytesPerBatch maximum update-object JSON bytes in one response
 * @param updateLifetimeMillis maximum age of the oldest unacknowledged update
 * @param eventsPerBatch maximum browser events processed per request
 * @param eventBytesPerBatch maximum UTF-8 bytes of an event batch
 */
public record ProtocolLimits(int pendingUpdates, int pendingBytes, int updatesPerBatch,
        int updateBytesPerBatch, long updateLifetimeMillis, int eventsPerBatch,
        int eventBytesPerBatch) {
    /**
     * Keeps the default transport envelope compatible with existing HTTP quotas.
     */
    public ProtocolLimits {
        if (pendingUpdates < 1 || pendingBytes < 1 || updatesPerBatch < 1
                || updatesPerBatch > pendingUpdates || updateBytesPerBatch < 1
                || updateBytesPerBatch > pendingBytes || updateLifetimeMillis < 1
                || eventsPerBatch < 1 || eventsPerBatch > 128
                || eventBytesPerBatch < 16 || eventBytesPerBatch > 512 * 1024) {
            throw new IllegalArgumentException("Invalid protocol queue limits");
        }
    }

    /**
     * @return default application-wide policy as applied independently to each page
     */
    public static ProtocolLimits defaults() {
        return new ProtocolLimits(32_768, 24 * 1024 * 1024, 1024, 8 * 1024 * 1024,
            60_000, 64, 512 * 1024);
    }
}
