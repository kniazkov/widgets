/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

/**
 * A reactive in-memory database whose records expose models that can be bound directly
 * to widgets.
 */
public interface Database extends AutoCloseable {
    /**
     * Creates a database builder.
     *
     * @return new builder
     */
    static DatabaseBuilder builder() {
        return new DatabaseBuilder();
    }

    /**
     * Returns a configured store.
     *
     * @param name store name
     * @return store
     */
    Store getStore(String name);

    /**
     * Stops the database dispatcher and closes its persistence backend.
     */
    @Override
    void close();
}
