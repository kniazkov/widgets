/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Cursor;

/**
 * Default model for a cursor.
 */
public final class CursorModel extends DefaultModel<Cursor> {
    /**
     * Creates a model with an automatic cursor.
     */
    public CursorModel() {
    }

    /**
     * Creates a model with a cursor.
     *
     * @param data initial cursor
     */
    public CursorModel(final Cursor data) {
        super(data);
    }

    @Override
    protected Cursor getDefaultData() {
        return Cursor.AUTO;
    }

    @Override
    public Model<Cursor> deriveWithData(final Cursor data) {
        return new CursorModel(data);
    }
}
