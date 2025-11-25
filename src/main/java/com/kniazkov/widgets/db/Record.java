/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.model.Model;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Represents a single database record in the primitive database layer.
 * <p>
 * A {@code Record} is essentially a typed container of reactive {@link Model} instances,
 * each associated with a {@link Field}. Different records may contain different sets of fields,
 * and fields are created lazily on demand.
 * <p>
 * Records are identified by a unique immutable {@link UUID}. Field values are stored
 * as reactive {@code Model} objects, allowing views, controllers, and other system
 * components to observe and react to changes.
 */
public abstract class Record {
    /**
     * A unique identifier of this record.
     */
    private final UUID id;

    /**
     * Internal storage for all field models belonging to this record.
     */
    protected final Map<String, Model<?>> data;

    /**
     * Creates a new record with the given identifier.
     *
     * @param id the unique identifier of this record (must not be {@code null})
     */
    Record(final UUID id) {
        this.id = id;
        this.data = new TreeMap<>();
    }

    /**
     * Returns the unique identifier of this record.
     *
     * @return the record's UUID
     */
    public UUID getId() {
        return this.id;
    }

    /**
     * Checks whether this record already contains a model for the given field.
     *
     * @param field the field to check
     * @return {@code true} if a model for the field exists, {@code false} otherwise
     */
    public boolean hasModel(final Field<?> field) {
        return this.data.containsKey(field.getName());
    }

    /**
     * Returns the reactive model associated with the given field.
     * <p>
     * If a model already exists, it is returned after verifying that the stored
     * value type matches the field's declared {@link Type}.
     * <br>
     * If no model exists yet, a new one is created using the field's type
     * ({@link Type#createModel()}), wrapped in a synchronized model, stored in this record,
     * and then returned.
     *
     * @param field the field whose model is requested
     * @param <T>   the data type associated with the field
     * @return the existing or newly created model for the field
     *
     * @throws IllegalStateException if an existing model stores data of an
     *  incompatible type (schematic mismatch)
     */
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

    /**
     * Creates a temporary editable copy of this record.
     *
     * @return an editable temporary record derived from this one
     */
    public Record edit() {
        return new TemporaryRecord(this);
    }

    /**
     * Saves this record according to the storage strategy defined by the subclass.
     */
    public abstract void save();
}
