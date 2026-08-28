/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.model.Model;
import java.util.UUID;

/**
 * An isolated set of record changes that can be committed or cancelled.
 */
public interface Draft {
    /**
     * Returns the future or edited record identifier.
     *
     * @return identifier
     */
    UUID getId();

    /**
     * Returns an editable model for a field.
     *
     * @param field field
     * @param <T> value type
     * @return draft model
     */
    <T> Model<T> model(Field<T> field);

    /**
     * Atomically applies this draft.
     *
     * @return committed record
     */
    DataRecord commit();

    /**
     * Discards this draft.
     */
    void cancel();
}
