/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence;

import java.util.List;

/**
 * Immutable database contents loaded by a persistence backend.
 */
public final class DatabaseSnapshot {
    /**
     * Stored records.
     */
    private final List<StoredRecord> records;

    /**
     * Creates a snapshot.
     *
     * @param records records
     */
    public DatabaseSnapshot(final List<StoredRecord> records) {
        this.records = List.copyOf(records);
    }

    /**
     * Creates an empty snapshot.
     *
     * @return empty snapshot
     */
    public static DatabaseSnapshot empty() {
        return new DatabaseSnapshot(List.of());
    }

    /**
     * Returns stored records.
     *
     * @return records
     */
    public List<StoredRecord> getRecords() {
        return this.records;
    }
}
