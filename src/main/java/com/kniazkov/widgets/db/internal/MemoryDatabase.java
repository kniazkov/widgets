/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.internal;

import com.kniazkov.widgets.db.Database;
import com.kniazkov.widgets.db.DataRecord;
import com.kniazkov.widgets.db.Draft;
import java.util.List;
import java.util.HashSet;
import com.kniazkov.widgets.db.Schema;
import com.kniazkov.widgets.db.Store;
import com.kniazkov.widgets.db.persistence.ChangeSet;
import com.kniazkov.widgets.db.persistence.DatabaseMetadata;
import com.kniazkov.widgets.db.persistence.DatabaseSnapshot;
import com.kniazkov.widgets.db.persistence.FieldMetadata;
import com.kniazkov.widgets.db.persistence.Persistence;
import com.kniazkov.widgets.db.persistence.PersistenceException;
import com.kniazkov.widgets.db.persistence.StoreMetadata;
import com.kniazkov.widgets.db.persistence.StoredRecord;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default database implementation backed by canonical objects in memory.
 */
public final class MemoryDatabase implements Database {
    /**
     * Database logger.
     */
    private static final Logger LOGGER = Logger.getLogger(
        MemoryDatabase.class.getName()
    );

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
     * Notifications deferred until all canonical records and views have been updated.
     */
    private List<Runnable> notifications;

    /**
     * Business validation may read but cannot write canonical state.
     */
    private boolean validating;

    /**
     * Prevents listener writes from interleaving publication of a committed batch.
     */
    private boolean publishing;

    /**
     * Storage outcome is uncertain; reopen is required.
     */
    private volatile boolean recoveryRequired;

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
        try {
            this.persistence.initialize(metadata(schemas));
            final DatabaseSnapshot snapshot = this.persistence.load();
            /*
             * Construction has not published this database yet. Loading on the
             * caller also prevents custom value converters from waiting for the
             * class whose static initializer is currently building the database.
             */
            this.load(snapshot);
        } catch (final RuntimeException | Error failure) {
            LOGGER.log(Level.SEVERE, "Unable to initialize database", failure);
            try {
                this.persistence.close();
            } catch (final RuntimeException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            this.dispatcher.close();
            throw failure;
        }
    }

    /**
     * Creates persistence-neutral metadata from configured schemas.
     *
     * @param schemas configured schemas
     * @return database metadata
     */
    private static DatabaseMetadata metadata(
        final Map<String, Schema> schemas
    ) {
        final ArrayList<StoreMetadata> stores = new ArrayList<>();
        int storePosition = 0;
        for (final Map.Entry<String, Schema> entry : schemas.entrySet()) {
            final ArrayList<FieldMetadata> fields = new ArrayList<>();
            int fieldPosition = 0;
            for (final com.kniazkov.widgets.db.Field<?> field
                : entry.getValue().getFields()) {
                fields.add(new FieldMetadata(
                    field.getName(),
                    field.getType().getName(),
                    field.getType().getStoredKind(),
                    field.getType().getStoredDefault(),
                    fieldPosition++,
                    field.getReferencedStore()
                ));
            }
            stores.add(new StoreMetadata(
                entry.getKey(),
                storePosition++,
                fields
            ));
        }
        return new DatabaseMetadata(
            DatabaseMetadata.CURRENT_FORMAT_VERSION,
            stores
        );
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
        this.requireWritable();
        try {
            this.persistence.commit(changes);
        } catch (final com.kniazkov.widgets.db.persistence.RecoveryRequiredException failure) {
            this.recoveryRequired = true;
            throw failure;
        }
    }

    /**
     * Queues a notification during publication preparation.
     *
     * @param action notification
     */
    void notifyAfterCommit(final Runnable action) {
        if (this.notifications == null) {
            action.run();
        } else {
            this.notifications.add(action);
        }
    }

    /**
     * Rejects reentrant mutations while observers receive a consistent snapshot.
     */
    void requireWritable() {
        if (this.publishing || this.validating) {
            throw new IllegalStateException("Cannot write from batch validation or notification");
        }
    }

    @Override
    public List<DataRecord> commit(
        final List<? extends Draft> drafts,
        final List<? extends DataRecord> removals,
        final Runnable validation
    ) {
        Objects.requireNonNull(validation, "validation");
        final List<? extends Draft> inputs = List.copyOf(drafts);
        final List<? extends DataRecord> deletes = List.copyOf(removals);
        return this.call(() -> {
            this.requireWritable();
            this.validating = true;
            try {
                validation.run();
            } finally {
                this.validating = false;
            }
            final List<ChangeSet.Mutation> mutations = new ArrayList<>();
            final List<Runnable> applications = new ArrayList<>();
            final List<DataRecord> results = new ArrayList<>();
            final HashSet<String> targets = new HashSet<>();
            for (final Draft input : inputs) {
                if (!(input instanceof MemoryDraft draft)
                    || !draft.getStore().belongsTo(this)) {
                    throw new IllegalArgumentException("Foreign draft");
                }
                draft.requireOpen();
                final MemoryStore store = draft.getStore();
                final StoredRecord saved = store.prepare(draft);
                requireUnique(targets, saved.getStore(), saved.getId());
                final MemoryRecord prepared = new MemoryRecord(store, saved);
                mutations.add(new ChangeSet.Upsert(saved));
                applications.add(() -> {
                    results.add(store.install(saved, prepared));
                    draft.markCommitted();
                });
            }
            for (final DataRecord input : deletes) {
                if (!(input instanceof MemoryRecord record)
                    || !record.getStore().belongsTo(this)) {
                    throw new IllegalArgumentException("Foreign record");
                }
                final MemoryStore store = record.getStore();
                store.requireCurrent(record);
                requireUnique(targets, store.getName(), record.getId());
                mutations.add(new ChangeSet.Delete(store.getName(), record.getId()));
                applications.add(() -> store.uninstall(record));
            }
            this.persist(new ChangeSet(mutations));
            this.notifications = new ArrayList<>();
            try {
                for (final Runnable application : applications) {
                    application.run();
                }
                for (final MemoryStore store : this.stores.values()) {
                    store.refreshBatch(mutations);
                }
                this.publishing = true;
                for (final Runnable notification : this.notifications) {
                    try {
                        notification.run();
                    } catch (final RuntimeException failure) {
                        LOGGER.log(Level.WARNING, "Database listener failed", failure);
                    }
                }
            } finally {
                this.publishing = false;
                this.notifications = null;
            }
            return List.copyOf(results);
        });
    }

    /**
     * Rejects ambiguous multiple mutations of one target.
     *
     * @param targets visited targets
     * @param store store name
     * @param id record identifier
     */
    private static void requireUnique(
        final HashSet<String> targets,
        final String store,
        final java.util.UUID id
    ) {
        if (!targets.add(store + ":" + id)) {
            throw new IllegalArgumentException("Duplicate batch target");
        }
    }

    /**
     * Verifies that the database is open.
     */
    private void requireOpen() {
        if (this.closed || this.recoveryRequired) {
            throw new IllegalStateException("Database is closed or requires recovery");
        }
    }

    @Override
    public void close() {
        try {
            this.dispatcher.run(() -> {
                if (!this.closed) {
                    this.requireWritable();
                    this.closed = true;
                    this.persistence.close();
                }
            });
        } catch (final RejectedExecutionException failure) {
            if (!this.closed) {
                throw failure;
            }
        } finally {
            if (this.closed) {
                this.dispatcher.close();
            }
        }
    }
}
