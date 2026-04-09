/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import java.util.List;

/**
 * Represents a database that manages named {@link Store} instances.
 * <p>
 * A {@code Database} acts as a registry and access point for stores that share
 * a common persistence backend or storage location. Implementations define how
 * stores are created, loaded, and flushed to the underlying medium.
 */
public abstract class Database {
    /**
     * Registers a store with the specified name and schema.
     * <p>
     * If the underlying implementation supports persistent storage, this method may
     * also load previously saved records for the store from that storage.
     *
     * @param name the unique store name
     * @param fields the schema fields supported by the store
     * @return this database instance
     */
    public abstract Database registerStore(String name, final List<Field<?>> fields);

    /**
     * Returns a previously registered store by name.
     *
     * @param name the store name
     * @return the registered store
     */
    public abstract Store getStore(String name);

    /**
     * Flushes the database state to persistent storage.
     * <p>
     * Depending on the implementation, this may write the entire database or only
     * pending changes.
     *
     * @return {@code true} if flushing succeeded; {@code false} otherwise
     */
    public abstract boolean flush();
}
