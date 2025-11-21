/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.model.Model;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class Record {
    private final UUID id;
    private final Map<String, Model<?>> data;

    public Record(final UUID id) {
        this.id = id;
        this.data = new TreeMap<>();
    }

    public UUID getId() {
        return this.id;
    }

    public <T> Model<T> getModel(final Field<T> field) {
        final String key = field.getName();
        final Model<?> existing = this.data.get(key);
        if (existing != null) {
            final Object value = existing.getData();
            final Class<T> expected = field.getValueClass();
            if (!expected.isInstance(value)) {
                throw new IllegalStateException(
                        "Field '" + key + "' has model with incompatible data type: "
                        + value.getClass().getName()
                        + " expected: " + expected.getName()
                );
            }
            @SuppressWarnings("unchecked")
            final Model<T> typed = (Model<T>) existing;
            return typed;
        } else {
            final Model<T> created = field.createModel().asSynchronized();
            this.data.put(key, created);
            return created;
        }
    }
}
