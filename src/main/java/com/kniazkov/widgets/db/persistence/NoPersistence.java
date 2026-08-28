/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence;

/**
 * Memory-only persistence backend.
 */
public final class NoPersistence implements Persistence {
    @Override
    public DatabaseSnapshot load() {
        return DatabaseSnapshot.empty();
    }

    @Override
    public void commit(final ChangeSet changes) {
        /*
         * Intentionally empty.
         */
    }

    @Override
    public void close() {
        /*
         * Intentionally empty.
         */
    }
}
