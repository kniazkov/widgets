/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.db.query.Query;
import com.kniazkov.widgets.model.Model;
import java.util.List;
import java.util.UUID;

/**
 * A named reactive collection of records with one schema.
 */
public interface Store {
    /**
     * Returns the store name.
     *
     * @return name
     */
    String getName();

    /**
     * Returns the store schema.
     *
     * @return schema
     */
    Schema getSchema();

    /**
     * Starts creation of a record.
     *
     * @return mutable draft
     */
    Draft createDraft();

    /**
     * Returns a record by identifier.
     *
     * @param id identifier
     * @return record, or {@code null}
     */
    DataRecord getRecord(UUID id);

    /**
     * Returns a stable snapshot of current records.
     *
     * @return records
     */
    List<DataRecord> getRecords();

    /**
     * Creates a live query result.
     *
     * @param query query
     * @return live record set
     */
    LiveRecordSet query(Query query);

    /**
     * Returns a model containing the current record count.
     *
     * @return count model
     */
    Model<Integer> getRecordCountModel();
}
