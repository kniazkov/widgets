/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistence-neutral typed representation of one record.
 */
public final class StoredRecord {
    /**
     * Store name.
     */
    private final String store;

    /**
     * Record identifier.
     */
    private final UUID id;

    /**
     * Creation time.
     */
    private final Instant createdAt;

    /**
     * Revision.
     */
    private final long revision;

    /**
     * Typed field values.
     */
    private final Map<String, StoredValue> fields;

    /**
     * Creates a stored record.
     *
     * @param store store name
     * @param id identifier
     * @param createdAt creation time
     * @param revision revision
     * @param fields typed fields
     */
    public StoredRecord(
        final String store,
        final UUID id,
        final Instant createdAt,
        final long revision,
        final Map<String, StoredValue> fields
    ) {
        this.store = Objects.requireNonNull(store, "store");
        this.id = Objects.requireNonNull(id, "id");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.revision = revision;
        final Map<String, StoredValue> values = new LinkedHashMap<>(
            Objects.requireNonNull(fields, "fields")
        );
        values.forEach((name, value) -> {
            Objects.requireNonNull(name, "field name");
            Objects.requireNonNull(value, "field value");
        });
        this.fields = Collections.unmodifiableMap(values);
    }

    /**
     * Returns the store name.
     *
     * @return store name
     */
    public String getStore() {
        return this.store;
    }

    /**
     * Returns the identifier.
     *
     * @return identifier
     */
    public UUID getId() {
        return this.id;
    }

    /**
     * Returns the creation time.
     *
     * @return creation time
     */
    public Instant getCreatedAt() {
        return this.createdAt;
    }

    /**
     * Returns the revision.
     *
     * @return revision
     */
    public long getRevision() {
        return this.revision;
    }

    /**
     * Returns typed field values.
     *
     * @return fields
     */
    public Map<String, StoredValue> getFields() {
        return this.fields;
    }
}
