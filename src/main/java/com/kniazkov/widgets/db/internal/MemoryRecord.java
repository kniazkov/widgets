/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.internal;

import com.kniazkov.widgets.db.DataRecord;
import com.kniazkov.widgets.db.Draft;
import com.kniazkov.widgets.db.Field;
import com.kniazkov.widgets.db.persistence.StoredRecord;
import com.kniazkov.widgets.model.Model;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Canonical in-memory record implementation.
 */
final class MemoryRecord implements DataRecord {
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
     * Canonical field models.
     */
    private final Map<Field<?>, StoredModel<?>> models;

    /**
     * Revision.
     */
    private long revision;

    /**
     * Creates a record from persisted data.
     *
     * @param store store
     * @param source persisted data
     */
    MemoryRecord(final MemoryStore store, final StoredRecord source) {
        this.store = store;
        this.id = source.getId();
        this.createdAt = source.getCreatedAt();
        this.revision = source.getRevision();
        this.models = new IdentityHashMap<>();
        this.applyFields(source.getFields());
    }

    /**
     * Applies encoded field values without persistence.
     *
     * @param values encoded values
     */
    void applyFields(final Map<String, String> values) {
        for (final Map.Entry<String, String> entry : values.entrySet()) {
            final Field<?> field = this.store.getSchema().getField(entry.getKey());
            if (field == null) {
                throw new IllegalStateException(
                    "Unknown persisted field '" + entry.getKey() + "'"
                );
            }
            this.applyEncoded(field, entry.getValue());
        }
    }

    /**
     * Applies one encoded field.
     *
     * @param field field
     * @param value encoded value
     * @param <T> value type
     */
    private <T> void applyEncoded(final Field<T> field, final String value) {
        final T decoded = field.getType().decode(value);
        final StoredModel<T> model = this.storedModel(field, decoded);
        model.apply(decoded);
    }

    /**
     * Returns or creates a canonical stored model.
     *
     * @param field field
     * @param initial initial value
     * @param <T> value type
     * @return stored model
     */
    private <T> StoredModel<T> storedModel(
        final Field<T> field,
        final T initial
    ) {
        final StoredModel<?> existing = this.models.get(field);
        if (existing == null) {
            final StoredModel<T> created = new StoredModel<>(this, field, initial);
            this.models.put(field, created);
            return created;
        }
        @SuppressWarnings("unchecked")
        final StoredModel<T> typed = (StoredModel<T>) existing;
        return typed;
    }

    /**
     * Updates one model through the store.
     *
     * @param model model
     * @param value new value
     * @param <T> value type
     * @return changed flag
     */
    <T> boolean update(final StoredModel<T> model, final T value) {
        return this.store.update(this, model, value);
    }

    /**
     * Creates a persisted snapshot, replacing one field hypothetically.
     *
     * @param changed changed field
     * @param value new value
     * @param nextRevision next revision
     * @param <T> value type
     * @return stored record
     */
    <T> StoredRecord snapshotWith(
        final Field<T> changed,
        final T value,
        final long nextRevision
    ) {
        final Map<String, String> fields = this.encodedFields();
        fields.put(changed.getName(), changed.getType().encode(value));
        return new StoredRecord(
            this.store.getName(),
            this.id,
            this.createdAt,
            nextRevision,
            fields
        );
    }

    /**
     * Returns a complete persisted snapshot.
     *
     * @return stored record
     */
    StoredRecord snapshot() {
        return new StoredRecord(
            this.store.getName(),
            this.id,
            this.createdAt,
            this.revision,
            this.encodedFields()
        );
    }

    /**
     * Encodes instantiated field models.
     *
     * @return encoded values
     */
    private Map<String, String> encodedFields() {
        final Map<String, String> values = new LinkedHashMap<>();
        for (final Field<?> field : this.store.getSchema().getFields()) {
            final StoredModel<?> model = this.models.get(field);
            if (model != null) {
                putEncoded(values, field, model);
            }
        }
        return values;
    }

    /**
     * Encodes one typed model.
     *
     * @param values destination
     * @param field field
     * @param model model
     * @param <T> value type
     */
    private static <T> void putEncoded(
        final Map<String, String> values,
        final Field<T> field,
        final StoredModel<?> model
    ) {
        @SuppressWarnings("unchecked")
        final StoredModel<T> typed = (StoredModel<T>) model;
        values.put(field.getName(), field.getType().encode(typed.getData()));
    }

    /**
     * Sets a committed revision.
     *
     * @param value revision
     */
    void setRevision(final long value) {
        this.revision = value;
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public Instant getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public long getRevision() {
        return this.store.call(() -> this.revision);
    }

    @Override
    public <T> Model<T> model(final Field<T> field) {
        return this.store.call(() -> {
            this.store.getSchema().require(field);
            return this.storedModel(
                field,
                field.getType().createModel().getData()
            );
        });
    }

    @Override
    public Draft edit() {
        return this.store.edit(this);
    }

    @Override
    public void remove() {
        this.store.remove(this);
    }
}
