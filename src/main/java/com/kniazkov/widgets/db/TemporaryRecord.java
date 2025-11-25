/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.model.Model;
import java.util.UUID;

public class TemporaryRecord extends Record {
    private final Record parent;

    public TemporaryRecord(Record parent) {
        super(UUID.randomUUID());
        this.parent = parent;

        for (final String key : parent.data.keySet()) {
            Model<?> model = parent.data.get(key);
            this.data.put(key, model.asCascading());
        }
    }

    @Override
    public void save() {
        for (final String key : this.data.keySet()) {
            Model<?> src = this.data.get(key);
            Model<?> dst = this.parent.data.get(key);
            if (dst == null) {
                this.parent.data.put(key, src);
            } else {
                dst.setObject(src.getData());
            }
        }
    }
}
