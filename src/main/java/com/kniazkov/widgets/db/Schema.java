/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable set of uniquely named fields.
 */
public final class Schema {
    /**
     * Fields in declaration order.
     */
    private final List<Field<?>> fields;

    /**
     * Fields indexed by name.
     */
    private final Map<String, Field<?>> byName;

    /**
     * Creates a schema.
     *
     * @param fields fields
     */
    private Schema(final List<Field<?>> fields) {
        final Map<String, Field<?>> index = new LinkedHashMap<>();
        for (final Field<?> field : fields) {
            Objects.requireNonNull(field, "field");
            if (index.putIfAbsent(field.getName(), field) != null) {
                throw new IllegalArgumentException(
                    "Duplicate field name: '" + field.getName() + "'"
                );
            }
        }
        this.fields = List.copyOf(fields);
        this.byName = Map.copyOf(index);
    }

    /**
     * Creates a schema from fields.
     *
     * @param fields fields
     * @return schema
     */
    public static Schema of(final Field<?>... fields) {
        return new Schema(List.of(fields));
    }

    /**
     * Returns fields in declaration order.
     *
     * @return fields
     */
    public List<Field<?>> getFields() {
        return this.fields;
    }

    /**
     * Returns a field by name.
     *
     * @param name field name
     * @return field, or {@code null}
     */
    public Field<?> getField(final String name) {
        return this.byName.get(name);
    }

    /**
     * Checks that a field belongs to this schema by identity.
     *
     * @param field field
     */
    public void require(final Field<?> field) {
        if (this.byName.get(field.getName()) != field) {
            throw new IllegalArgumentException(
                "Field '" + field.getName() + "' does not belong to this schema"
            );
        }
    }
}
