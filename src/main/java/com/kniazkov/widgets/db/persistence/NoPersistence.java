/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence;

import java.util.Objects;

/**
 * Memory-only persistence backend.
 */
public final class NoPersistence implements Persistence {
    /**
     * Creates a memory-only persistence backend.
     */
    public NoPersistence() {
    }

    @Override
    public void initialize(final DatabaseMetadata metadata) {
        Objects.requireNonNull(metadata, "metadata");
    }

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
