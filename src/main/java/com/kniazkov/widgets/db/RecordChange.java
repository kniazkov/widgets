/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

/**
 * Describes how one record changed inside a live result.
 *
 * @param kind change kind
 * @param record affected record
 * @param oldIndex previous index, or {@code -1}
 * @param newIndex current index, or {@code -1}
 */
public record RecordChange(
    Kind kind,
    DataRecord record,
    int oldIndex,
    int newIndex
) {
    /**
     * Supported change kinds.
     */
    public enum Kind {
        /**
         * A record entered the result.
         */
        ADDED,

        /**
         * A record left the result.
         */
        REMOVED,

        /**
         * A matching record changed without moving.
         */
        UPDATED,

        /**
         * A matching record changed its position.
         */
        MOVED
    }
}
