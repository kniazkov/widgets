/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.internal;

import com.kniazkov.widgets.db.DataRecord;
import com.kniazkov.widgets.db.Field;
import com.kniazkov.widgets.db.LiveRecordSet;
import com.kniazkov.widgets.db.RecordChange;
import com.kniazkov.widgets.db.RecordSetListener;
import com.kniazkov.widgets.db.Subscription;
import com.kniazkov.widgets.db.query.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Live query result maintained by a {@link MemoryStore}.
 */
final class MemoryLiveRecordSet implements LiveRecordSet {
    /**
     * Store.
     */
    private final MemoryStore store;

    /**
     * Query.
     */
    private final Query query;

    /**
     * Fields affecting membership or order.
     */
    private final Set<Field<?>> dependencies;

    /**
     * Current ordered records.
     */
    private List<MemoryRecord> records;

    /**
     * Listener set.
     */
    private final Set<RecordSetListener> listeners;

    /**
     * Creates a live result.
     *
     * @param store store
     * @param query query
     */
    MemoryLiveRecordSet(final MemoryStore store, final Query query) {
        this.store = store;
        this.query = query;
        this.dependencies = query.dependencies();
        this.records = this.evaluate();
        this.listeners = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    /**
     * Evaluates the query.
     *
     * @return matching records
     */
    private List<MemoryRecord> evaluate() {
        final List<MemoryRecord> result = new ArrayList<>();
        for (final MemoryRecord record : this.store.currentRecords()) {
            if (this.query.matches(record)) {
                result.add(record);
            }
        }
        result.sort(this.query.comparator());
        return result;
    }

    /**
     * Refreshes this result after one record changes.
     *
     * @param affected affected record
     * @param field changed field, or {@code null}
     */
    void refresh(final MemoryRecord affected, final Field<?> field) {
        final int oldIndex = this.records.indexOf(affected);
        if (field != null && !this.dependencies.contains(field)) {
            if (oldIndex >= 0) {
                this.emit(
                    new RecordChange(
                        RecordChange.Kind.UPDATED,
                        affected,
                        oldIndex,
                        oldIndex
                    )
                );
            }
            return;
        }
        final List<MemoryRecord> updated = this.evaluate();
        final int newIndex = updated.indexOf(affected);
        this.records = updated;
        final RecordChange change;
        if (oldIndex < 0 && newIndex >= 0) {
            change = new RecordChange(
                RecordChange.Kind.ADDED,
                affected,
                -1,
                newIndex
            );
        } else if (oldIndex >= 0 && newIndex < 0) {
            change = new RecordChange(
                RecordChange.Kind.REMOVED,
                affected,
                oldIndex,
                -1
            );
        } else if (oldIndex >= 0 && oldIndex != newIndex) {
            change = new RecordChange(
                RecordChange.Kind.MOVED,
                affected,
                oldIndex,
                newIndex
            );
        } else if (newIndex >= 0) {
            change = new RecordChange(
                RecordChange.Kind.UPDATED,
                affected,
                newIndex,
                newIndex
            );
        } else {
            return;
        }
        this.emit(change);
    }

    /**
     * Recomputes the final view once and emits changes against its previous snapshot.
     *
     * @param affected identifiers mutated by the batch
     */
    void refreshBatch(final Set<java.util.UUID> affected) {
        if (affected.isEmpty()) {
            return;
        }
        final List<MemoryRecord> previous = this.records;
        this.records = this.evaluate();
        final Set<MemoryRecord> candidates = new java.util.LinkedHashSet<>(previous);
        candidates.addAll(this.records);
        for (final MemoryRecord record : candidates) {
            final int before = previous.indexOf(record);
            final int after = this.records.indexOf(record);
            if (!affected.contains(record.getId()) && before == after) {
                continue;
            }
            final RecordChange.Kind kind;
            if (before < 0) {
                kind = RecordChange.Kind.ADDED;
            } else if (after < 0) {
                kind = RecordChange.Kind.REMOVED;
            } else if (before != after) {
                kind = RecordChange.Kind.MOVED;
            } else {
                kind = RecordChange.Kind.UPDATED;
            }
            this.emit(new RecordChange(kind, record, before, after));
        }
    }

    /**
     * Notifies current listeners.
     *
     * @param change change
     */
    private void emit(final RecordChange change) {
        for (final RecordSetListener listener : List.copyOf(this.listeners)) {
            this.store.notifyAfterCommit(() -> listener.accept(change));
        }
    }

    @Override
    public List<DataRecord> getRecords() {
        return this.store.call(() -> new ArrayList<>(this.records));
    }

    @Override
    public Subscription subscribe(final RecordSetListener listener) {
        final RecordSetListener checked = Objects.requireNonNull(
            listener,
            "listener"
        );
        this.store.run(() -> this.listeners.add(checked));
        return () -> this.store.run(() -> this.listeners.remove(checked));
    }
}
