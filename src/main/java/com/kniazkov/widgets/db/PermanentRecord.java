/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import java.util.UUID;

public class PermanentRecord extends Record {
    private final Store store;

    public PermanentRecord(final UUID id, final Store store) {
        super(id);
        this.store = store;
    }

    @Override
    public void save() {
        this.store.save();
    }
}
