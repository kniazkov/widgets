/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * An atomic group of persistence mutations.
 */
public final class ChangeSet {
    /**
     * Base mutation type.
     */
    public sealed interface Mutation permits Upsert, Delete {
    }

    /**
     * Inserts or replaces one complete record.
     *
     * @param record record
     */
    public record Upsert(StoredRecord record) implements Mutation {
        /**
         * Validates the mutation.
         */
        public Upsert {
            Objects.requireNonNull(record, "record");
        }
    }

    /**
     * Deletes one record.
     *
     * @param store store name
     * @param id record identifier
     */
    public record Delete(String store, UUID id) implements Mutation {
        /**
         * Validates the mutation.
         */
        public Delete {
            Objects.requireNonNull(store, "store");
            Objects.requireNonNull(id, "id");
        }
    }

    /**
     * Mutations.
     */
    private final List<Mutation> mutations;

    /**
     * Creates a change set.
     *
     * @param mutations mutations
     */
    public ChangeSet(final List<Mutation> mutations) {
        this.mutations = List.copyOf(mutations);
    }

    /**
     * Creates a one-record upsert.
     *
     * @param record record
     * @return change set
     */
    public static ChangeSet upsert(final StoredRecord record) {
        return new ChangeSet(List.of(new Upsert(record)));
    }

    /**
     * Creates a one-record deletion.
     *
     * @param store store name
     * @param id identifier
     * @return change set
     */
    public static ChangeSet delete(final String store, final UUID id) {
        return new ChangeSet(List.of(new Delete(store, id)));
    }

    /**
     * Returns mutations.
     *
     * @return mutations
     */
    public List<Mutation> getMutations() {
        return this.mutations;
    }
}
