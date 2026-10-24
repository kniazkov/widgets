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
    private final DatabaseModel<Integer> count;

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
        this.count = new DatabaseModel<>(this, new IntegerModel());
    }

    /**
     * @param owner expected database
     * @return ownership match
     */
    boolean belongsTo(final MemoryDatabase owner) {
        return this.database == owner;
    }

    /**
     * @param action deferred observer
     */
    void notifyAfterCommit(final Runnable action) {
        this.database.notifyAfterCommit(action);
    }

    /**
     * Removes a prevalidated record after commit.
     *
     * @param record record
     */
    void uninstall(final MemoryRecord record) {
        this.records.remove(record.getId());
        this.count.setData(this.records.size());
    }

    /**
     * Refreshes all views after the complete batch is installed.
     *
     * @param mutations committed mutations
     */
    void refreshBatch(final List<ChangeSet.Mutation> mutations) {
        final Set<UUID> affected = new java.util.HashSet<>();
        for (final ChangeSet.Mutation mutation : mutations) {
            if (mutation instanceof ChangeSet.Upsert upsert
                && this.name.equals(upsert.record().getStore())) {
                affected.add(upsert.record().getId());
            } else if (mutation instanceof ChangeSet.Delete delete
                && this.name.equals(delete.store())) {
                affected.add(delete.id());
            }
        }
        for (final MemoryLiveRecordSet view : List.copyOf(this.views)) {
            view.refreshBatch(affected);
        }
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
        final StoredRecord stored = this.prepare(draft);
        final MemoryRecord prepared = new MemoryRecord(this, stored);
        this.database.persist(ChangeSet.upsert(stored));
        final MemoryRecord committed = this.install(stored, prepared);
        this.refreshViews(committed, null);
        return committed;
    }

    /**
     * Validates ownership-independent revision constraints without changing state.
     *
     * @param draft input
     * @return prepared snapshot
     */
    StoredRecord prepare(final MemoryDraft draft) {
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
        return draft.snapshot(revision);
    }

    /**
     * Installs a prevalidated snapshot after persistence.
     *
     * @param stored snapshot
     * @param prepared converted new record
     * @return canonical record
     */
    MemoryRecord install(final StoredRecord stored, final MemoryRecord prepared) {
        final MemoryRecord current = this.records.get(stored.getId());
        final MemoryRecord committed;
        if (current == null) {
            committed = prepared;
            this.records.put(committed.getId(), committed);
            this.count.setData(this.records.size());
        } else {
            current.applyPrepared(prepared);
            current.setRevision(stored.getRevision());
            committed = current;
        }
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
            if (model.getField().getType().toStoredValue(model.getData()).equals(
                model.getField().getType().toStoredValue(value))) {
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
    void requireCurrent(final MemoryRecord record) {
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
