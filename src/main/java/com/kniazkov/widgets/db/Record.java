/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.model.Model;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public abstract class Record {
    private final UUID id;

    protected final Map<String, Model<?>> data;

    Record(final UUID id) {
        this.id = id;
        this.data = new TreeMap<>();
    }

    public UUID getId() {
        return this.id;
    }

    public boolean hasModel(final Field<?> field) {
        return this.data.containsKey(field.getName());
    }

    public <T> Model<T> getModel(final Field<T> field) {
        final String key = field.getName();
        final Model<?> existing = this.data.get(key);
        if (existing != null) {
            final Object value = existing.getData();
            final Class<T> expected = field.getType().getValueClass();
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
            final Model<T> created = field.getType().createModel().asSynchronized();
            this.data.put(key, created);
            return created;
        }
    }

    public Record edit() {
        return new TemporaryRecord(this);
    }

    public abstract void save();
}
