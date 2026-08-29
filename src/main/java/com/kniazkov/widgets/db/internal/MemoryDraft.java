/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.internal;

import com.kniazkov.widgets.db.DataRecord;
import com.kniazkov.widgets.db.Draft;
import com.kniazkov.widgets.db.Field;
import com.kniazkov.widgets.db.persistence.StoredRecord;
import com.kniazkov.widgets.db.persistence.StoredValue;
import com.kniazkov.widgets.model.Model;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Isolated draft implementation.
 */
final class MemoryDraft implements Draft {
    /**
     * Owning store.
     */
    private final MemoryStore store;

    /**
     * Identifier.
     */
    private final UUID id;

    /**
     * Creation time.
     */
    private final Instant createdAt;

    /**
     * Base revision, or {@code -1} for a new record.
     */
    private final long baseRevision;

    /**
     * Stored values inherited from the record.
     */
    private final Map<String, StoredValue> stored;

    /**
     * Editable models created on demand.
     */
    private final Map<Field<?>, Model<?>> models;

    /**
     * Closed flag.
     */
    private boolean closed;

    /**
     * Creates a new-record draft.
     *
     * @param store store
     */
    MemoryDraft(final MemoryStore store) {
        this.store = store;
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.baseRevision = -1L;
        this.stored = new LinkedHashMap<>();
        this.models = new IdentityHashMap<>();
    }

    /**
     * Creates an edit draft.
     *
     * @param store store
     * @param source source record
     */
    MemoryDraft(final MemoryStore store, final MemoryRecord source) {
        final StoredRecord snapshot = source.snapshot();
        this.store = store;
        this.id = source.getId();
        this.createdAt = source.getCreatedAt();
        this.baseRevision = source.getRevision();
        this.stored = new LinkedHashMap<>(snapshot.getFields());
        this.models = new IdentityHashMap<>();
    }

    /**
     * Returns the base revision.
     *
     * @return revision, or {@code -1}
     */
    long getBaseRevision() {
        return this.baseRevision;
    }

    /**
     * Checks whether this edits an existing record.
     *
     * @return existing flag
     */
    boolean editsExisting() {
        return this.baseRevision >= 0L;
    }

    /**
     * Creates a persistence snapshot.
     *
     * @param revision committed revision
     * @return stored record
     */
    StoredRecord snapshot(final long revision) {
        final Map<String, StoredValue> values = new LinkedHashMap<>(this.stored);
        for (final Map.Entry<Field<?>, Model<?>> entry : this.models.entrySet()) {
            putStored(values, entry.getKey(), entry.getValue());
        }
        return new StoredRecord(
            this.store.getName(),
            this.id,
            this.createdAt,
            revision,
            values
        );
    }

    /**
     * Converts a typed model to a stored value.
     *
     * @param destination destination
     * @param field field
     * @param model model
     * @param <T> value type
     */
    private static <T> void putStored(
        final Map<String, StoredValue> destination,
        final Field<T> field,
        final Model<?> model
    ) {
        @SuppressWarnings("unchecked")
        final Model<T> typed = (Model<T>) model;
        destination.put(
            field.getName(),
            field.getType().toStoredValue(typed.getData())
        );
    }

    /**
     * Verifies that the draft is open.
     */
    private void requireOpen() {
        if (this.closed) {
            throw new IllegalStateException("Draft is closed");
        }
    }

    /**
     * Marks this draft closed.
     */
    void markCommitted() {
        this.closed = true;
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public <T> Model<T> model(final Field<T> field) {
        return this.store.call(() -> {
            this.requireOpen();
            this.store.getSchema().require(field);
            final Model<?> existing = this.models.get(field);
            if (existing != null) {
                @SuppressWarnings("unchecked")
                final Model<T> typed = (Model<T>) existing;
                return typed;
            }
            final StoredValue saved = this.stored.get(field.getName());
            final Model<T> created = saved == null
                ? field.getType().createModel().asSynchronized()
                : field.getType().createModel(
                    field.getType().fromStoredValue(saved)
                ).asSynchronized();
            this.models.put(field, created);
            return created;
        });
    }

    @Override
    public DataRecord commit() {
        return this.store.call(() -> {
            this.requireOpen();
            final DataRecord result = this.store.commit(this);
            this.markCommitted();
            return result;
        });
    }

    @Override
    public void cancel() {
        this.store.run(() -> {
            this.requireOpen();
            this.closed = true;
        });
    }
}
