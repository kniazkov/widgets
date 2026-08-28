/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import java.util.List;

/**
 * A query result that keeps its membership and ordering synchronized with a store.
 */
public interface LiveRecordSet {
    /**
     * Returns a snapshot of matching records.
     *
     * @return matching records
     */
    List<DataRecord> getRecords();

    /**
     * Subscribes to structural and value changes.
     *
     * @param listener listener
     * @return subscription used to detach the listener
     */
    Subscription subscribe(RecordSetListener listener);
}
