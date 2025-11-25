/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import java.util.UUID;

/**
 * A concrete {@link Record} that represents a persistent database entry.
 * <p>
 * Unlike {@link TemporaryRecord}, which is used for staged or intermediate editing,
 * a {@code PermanentRecord} is directly backed by a {@link Store} — an abstraction
 * responsible for writing changes to the underlying storage medium (memory, file, database,
 * remote service, etc.).
 * <p>
 * The record’s lifecycle is tied to the associated {@code Store}. Whenever {@link #save()}
 * is called, the record delegates the persistence operation to the store, ensuring that all
 * reactive model values are flushed to permanent storage.
 */
public class PermanentRecord extends Record {
    /**
     * The storage backend that manages persistence for this record.
     */
    private final Store store;

    /**
     * Creates a new permanent record with the given identifier and backing store.
     *
     * @param id    the unique identifier of the record
     * @param store the storage backend responsible for saving this record
     */
    public PermanentRecord(final UUID id, final Store store) {
        super(id);
        this.store = store;
    }

    /**
     * Saves this record by delegating the operation to the backing {@link Store}.
     */
    @Override
    public void save() {
        this.store.save();
    }
}
