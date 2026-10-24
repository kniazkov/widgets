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
     * Atomically commits drafts and removes records across this database's stores.
     * Drafts must be open and their revisions current. Duplicate targets and foreign
     * objects are rejected before persistence. On success, drafts are closed and results
     * follow draft order. Prepare drafts without network calls inside database listeners.
     *
     * @param drafts new or edited records
     * @param removals records to remove
     * @return committed canonical records
     */
    default java.util.List<DataRecord> commit(
        java.util.List<? extends Draft> drafts,
        java.util.List<? extends DataRecord> removals
    ) {
        return this.commit(drafts, removals, () -> { });
    }

    /**
     * Validates business preconditions on the database dispatcher, then commits a batch
     * without allowing intervening writes. Validation may read records and throw to
     * abort, but must not mutate canonical records, perform network calls or wait for
     * other threads. Draft revision checks still apply after validation.
     *
     * @param drafts new or edited records
     * @param removals records to remove
     * @param validation read-only business validation
     * @return committed records in draft order
     */
    default java.util.List<DataRecord> commit(
        java.util.List<? extends Draft> drafts,
        java.util.List<? extends DataRecord> removals,
        Runnable validation
    ) {
        throw new UnsupportedOperationException("Atomic batches are not supported");
    }

    /**
     * Stops the database dispatcher and closes its persistence backend.
     */
    @Override
    void close();
}
