/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.model.Model;
import java.time.Instant;
import java.util.UUID;

/**
 * A canonical persistent record held by a {@link Store}.
 */
public interface DataRecord {
    /**
     * Returns the record identifier.
     *
     * @return identifier
     */
    UUID getId();

    /**
     * Returns the creation time.
     *
     * @return creation time
     */
    Instant getCreatedAt();

    /**
     * Returns the current revision.
     *
     * @return revision
     */
    long getRevision();

    /**
     * Returns the canonical model for a field.
     *
     * @param field field
     * @param <T> value type
     * @return model
     */
    <T> Model<T> model(Field<T> field);

    /**
     * Starts an isolated edit of this record.
     *
     * @return draft
     */
    Draft edit();

    /**
     * Removes this record from its store.
     */
    void remove();
}
