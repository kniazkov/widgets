/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence;

/**
 * Stores committed database changes and loads the state used to seed RAM.
 * {@link #initialize(DatabaseMetadata)} is called before loading or committing.
 */
public interface Persistence extends AutoCloseable {
    /**
     * Creates or validates the persisted schema catalog.
     *
     * @param metadata configured database metadata
     */
    void initialize(DatabaseMetadata metadata);

    /**
     * Loads all known records.
     *
     * @return snapshot
     */
    DatabaseSnapshot load();

    /**
     * Atomically persists a set of changes.
     *
     * @param changes changes
     */
    void commit(ChangeSet changes);

    /**
     * Releases persistence resources.
     */
    @Override
    void close();
}
