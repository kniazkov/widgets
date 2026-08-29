/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.internal;

import com.kniazkov.widgets.db.Database;
import com.kniazkov.widgets.db.Schema;
import com.kniazkov.widgets.db.Store;
import com.kniazkov.widgets.db.persistence.ChangeSet;
import com.kniazkov.widgets.db.persistence.DatabaseSnapshot;
import com.kniazkov.widgets.db.persistence.Persistence;
import com.kniazkov.widgets.db.persistence.PersistenceException;
import com.kniazkov.widgets.db.persistence.StoredRecord;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;

/**
 * Default database implementation backed by canonical objects in memory.
 */
public final class MemoryDatabase implements Database {
    /**
     * Serial dispatcher.
     */
    private final SerialDispatcher dispatcher;

    /**
     * Persistence backend.
     */
    private final Persistence persistence;

    /**
     * Configured stores.
     */
    private final Map<String, MemoryStore> stores;

    /**
     * Closed flag.
     */
    private volatile boolean closed;

    /**
     * Creates and loads a database.
     *
     * @param schemas configured schemas
     * @param persistence persistence backend
     */
    @SuppressWarnings("this-escape")
    public MemoryDatabase(
        final Map<String, Schema> schemas,
        final Persistence persistence
    ) {
        this.dispatcher = new SerialDispatcher();
        this.persistence = Objects.requireNonNull(persistence, "persistence");
        this.stores = new LinkedHashMap<>();
        for (final Map.Entry<String, Schema> entry : schemas.entrySet()) {
            this.stores.put(
                entry.getKey(),
                new MemoryStore(this, entry.getKey(), entry.getValue())
            );
        }
        final DatabaseSnapshot snapshot = this.persistence.load();
        this.dispatcher.run(() -> this.load(snapshot));
    }

    /**
     * Loads one snapshot into configured stores.
     *
     * @param snapshot snapshot
     */
    private void load(final DatabaseSnapshot snapshot) {
        for (final StoredRecord record : snapshot.getRecords()) {
            final MemoryStore store = this.stores.get(record.getStore());
            if (store == null) {
                throw new PersistenceException(
                    "Persistence contains unknown store '" + record.getStore() + "'"
                );
            }
            store.load(record);
        }
    }

    @Override
    public Store getStore(final String name) {
        final MemoryStore store = this.stores.get(name);
        if (store == null) {
            throw new IllegalArgumentException("Unknown store '" + name + "'");
        }
        return store;
    }

    /**
     * Executes a database operation.
     *
     * @param action operation
     * @param <T> result type
     * @return result
     */
    <T> T call(final java.util.concurrent.Callable<T> action) {
        this.requireOpen();
        try {
            return this.dispatcher.call(() -> {
                this.requireOpen();
                return action.call();
            });
        } catch (final RejectedExecutionException err) {
            if (this.closed) {
                throw new IllegalStateException("Database is closed", err);
            }
            throw err;
        }
    }

    /**
     * Executes a database operation.
     *
     * @param action operation
     */
    void run(final Runnable action) {
        this.call(() -> {
            action.run();
            return null;
        });
    }

    /**
     * Persists changes before publishing them in memory.
     *
     * @param changes changes
     */
    void persist(final ChangeSet changes) {
        this.persistence.commit(changes);
    }

    /**
     * Verifies that the database is open.
     */
    private void requireOpen() {
        if (this.closed) {
            throw new IllegalStateException("Database is closed");
        }
    }

    @Override
    public synchronized void close() {
        if (this.closed) {
            return;
        }
        this.dispatcher.run(() -> {
            this.closed = true;
            this.persistence.close();
        });
        this.dispatcher.close();
    }
}
