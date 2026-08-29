/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

/**
 * Receives changes from a {@link LiveRecordSet}.
 */
@FunctionalInterface
public interface RecordSetListener {
    /**
     * Handles one change.
     *
     * @param change change
     */
    void accept(RecordChange change);
}
