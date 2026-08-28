/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.internal;

import com.kniazkov.widgets.db.ConflictException;
import com.kniazkov.widgets.db.DataRecord;
import com.kniazkov.widgets.db.Draft;
import com.kniazkov.widgets.db.Field;
import com.kniazkov.widgets.db.LiveRecordSet;
import com.kniazkov.widgets.db.Schema;
import com.kniazkov.widgets.db.Store;
import com.kniazkov.widgets.db.persistence.ChangeSet;
import com.kniazkov.widgets.db.persistence.StoredRecord;
import com.kniazkov.widgets.db.query.Query;
import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.SynchronizedModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

/**
 * Default in-memory store implementation.
 */
final class MemoryStore implements Store {
    /**
     * Owning database.
     */
    private final MemoryDatabase database;

    /**
     * Store name.
     */
    private final String name;

    /**
     * Store schema.
     */
    private final Schema schema;

    /**
     * Canonical records.
     */
    private final Map<UUID, MemoryRecord> records;

    /**
     * Live views held weakly.
     */
    private final Set<MemoryLiveRecordSet> views;

    /**
     * Reactive record count.
     */
    private final SynchronizedModel<Integer> count;

    /**
     * Creates a store.
     *
     * @param database database
     * @param name store name
     * @param schema schema
     */
    MemoryStore(
        final MemoryDatabase database,
        final String name,
        final Schema schema
    ) {
        this.database = database;
        this.name = name;
        this.schema = schema;
        this.records = new LinkedHashMap<>();
        this.views = Collections.newSetFromMap(new WeakHashMap<>());
        this.count = new IntegerModel().asSynchronized();
    }

    /**
     * Loads a record before the database becomes visible.
     *
     * @param source stored record
     */
    void load(final StoredRecord source) {
        if (!this.name.equals(source.getStore())) {
            throw new IllegalArgumentException("Record belongs to another store");
        }
        if (this.records.containsKey(source.getId())) {
            throw new IllegalStateException(
                "Duplicate record identifier " + source.getId()
            );
        }
        this.records.put(source.getId(), new MemoryRecord(this, source));
        this.count.setData(this.records.size());
    }

    /**
     * Executes an operation on the database thread.
     *
     * @param action operation
     * @param <T> result type
     * @return result
     */
    <T> T call(final Callable<T> action) {
        return this.database.call(action);
    }

    /**
     * Executes an operation on the database thread.
     *
     * @param action operation
     */
    void run(final Runnable action) {
        this.database.run(action);
    }

    /**
     * Returns current records for live-query evaluation.
     *
     * @return records
     */
    List<MemoryRecord> currentRecords() {
        return new ArrayList<>(this.records.values());
    }

    /**
     * Creates an edit draft.
     *
     * @param record source record
     * @return draft
     */
    Draft edit(final MemoryRecord record) {
        return this.call(() -> {
            this.requireCurrent(record);
            return new MemoryDraft(this, record);
        });
    }

    /**
     * Commits a draft.
     *
     * @param draft draft
     * @return committed record
     */
    DataRecord commit(final MemoryDraft draft) {
        final MemoryRecord current = this.records.get(draft.getId());
        final long revision;
        if (draft.editsExisting()) {
            if (current == null
                || current.getRevision() != draft.getBaseRevision()) {
                throw new ConflictException(
                    "Record " + draft.getId() + " was changed while being edited"
                );
            }
            revision = current.getRevision() + 1L;
        } else {
            if (current != null) {
                throw new ConflictException(
                    "Record " + draft.getId() + " already exists"
                );
            }
            revision = 1L;
        }
        final StoredRecord stored = draft.snapshot(revision);
        this.database.persist(ChangeSet.upsert(stored));
        final MemoryRecord committed;
        if (current == null) {
            committed = new MemoryRecord(this, stored);
            this.records.put(committed.getId(), committed);
            this.count.setData(this.records.size());
        } else {
            current.applyFields(stored.getFields());
            current.setRevision(revision);
            committed = current;
        }
        this.refreshViews(committed, null);
        return committed;
    }

    /**
     * Commits a direct field-model update.
     *
     * @param record record
     * @param model model
     * @param value new value
     * @param <T> value type
     * @return changed flag
     */
    <T> boolean update(
        final MemoryRecord record,
        final StoredModel<T> model,
        final T value
    ) {
        Objects.requireNonNull(value, "value");
        return this.call(() -> {
            this.requireCurrent(record);
            if (Objects.equals(model.getData(), value)) {
                return false;
            }
            final long revision = record.getRevision() + 1L;
            final StoredRecord stored = record.snapshotWith(
                model.getField(),
                value,
                revision
            );
            this.database.persist(ChangeSet.upsert(stored));
            model.apply(value);
            record.setRevision(revision);
            this.refreshViews(record, model.getField());
            return true;
        });
    }

    /**
     * Removes a record.
     *
     * @param record record
     */
    void remove(final MemoryRecord record) {
        this.run(() -> {
            this.requireCurrent(record);
            this.database.persist(ChangeSet.delete(this.name, record.getId()));
            this.records.remove(record.getId());
            this.count.setData(this.records.size());
            this.refreshViews(record, null);
        });
    }

    /**
     * Verifies record identity and ownership.
     *
     * @param record record
     */
    private void requireCurrent(final MemoryRecord record) {
        if (this.records.get(record.getId()) != record) {
            throw new IllegalStateException(
                "Record " + record.getId() + " is not in this store"
            );
        }
    }

    /**
     * Refreshes live views.
     *
     * @param record affected record
     * @param field changed field, or {@code null}
     */
    private void refreshViews(
        final MemoryRecord record,
        final Field<?> field
    ) {
        for (final MemoryLiveRecordSet view : List.copyOf(this.views)) {
            view.refresh(record, field);
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Schema getSchema() {
        return this.schema;
    }

    @Override
    public Draft createDraft() {
        return this.call(() -> new MemoryDraft(this));
    }

    @Override
    public DataRecord getRecord(final UUID id) {
        return this.call(() -> this.records.get(id));
    }

    @Override
    public List<DataRecord> getRecords() {
        return this.call(() -> new ArrayList<>(this.records.values()));
    }

    @Override
    public LiveRecordSet query(final Query query) {
        return this.call(() -> {
            final MemoryLiveRecordSet result = new MemoryLiveRecordSet(
                this,
                Objects.requireNonNull(query, "query")
            );
            this.views.add(result);
            return result;
        });
    }

    @Override
    public Model<Integer> getRecordCountModel() {
        return this.count;
    }
}
