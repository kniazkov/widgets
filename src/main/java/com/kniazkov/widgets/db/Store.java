package com.kniazkov.widgets.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Represents a storage backend that manages a collection of {@link Record} objects.
 * <p>
 * A {@code Store} defines:
 * <ul>
 *     <li>a fixed list of available {@link Field fields},</li>
 *     <li>a map of persistent {@link Record records},</li>
 *     <li>the logic required to save all managed records.</li>
 * </ul>
 *
 * <p>
 * Concrete subclasses describe how and where data is persisted — for example,
 * in memory, on disk, or via a remote protocol. The {@code Store} itself
 * provides fundamental record management operations, including creation and lookup.
 */
public abstract class Store {
    /**
     * The list of fields supported by this store.
     * <p>
     * All records managed by this store may contain any subset of these fields,
     * and the list is immutable after construction.
     */
    private final List<Field<?>> fields;

    /**
     * The map of all persistent records stored in this backend,
     * keyed by their unique {@link UUID}.
     * <p>
     * Only {@link PermanentRecord} instances live here; temporary records modify data through
     * their parent and do not appear in this map.
     */
    private final Map<UUID, PermanentRecord> records;

    /**
     * Creates a new store with the specified list of fields.
     *
     * @param fields the list of fields describing the schema for this store
     */
    public Store(final List<Field<?>> fields) {
        this.fields = Collections.unmodifiableList(fields);
        this.records = new TreeMap<>();
    }

    /**
     * Returns the list of fields defined for this store.
     *
     * @return an unmodifiable list of field descriptors
     */
    public List<Field<?>> getFields() {
        return this.fields;
    }

    /**
     * Creates a new record with a randomly generated {@link UUID}.
     *
     * @return the newly created record
     */
    public Record createRecord() {
        return this.createRecord(UUID.randomUUID());
    }

    /**
     * Creates a new record with the specified unique identifier.
     *
     * @param id the UUID to assign to the new record
     * @return the newly created persistent record
     */
    public Record createRecord(final UUID id) {
        final PermanentRecord record = new PermanentRecord(id, this);
        this.records.put(id, record);
        return record;
    }

    /**
     * Returns all records currently stored in this backend.
     * <p>
     * The returned list is a defensive copy so that callers cannot mutate
     * the underlying storage.
     *
     * @return a list of all stored records
     */
    public List<Record> getAllRecords() {
        return new ArrayList<>(this.records.values());
    }

    /**
     * Looks up a record by its unique identifier.
     *
     * @param id the record ID
     * @return the record with the given ID, or {@code null} if not found
     */
    public Record getRecordById(final UUID id) {
        return this.records.get(id);
    }

    /**
     * Saves all records managed by this store.
     * <p>
     * The implementation defines what “saving” means —
     * writing to disk, syncing to remote storage, flushing caches, etc.
     */
    public abstract void save();
}
