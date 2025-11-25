/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

/**
 * Describes a single field in the primitive database schema.
 *
 * @param <T> the Java type of data represented by this field
 */
public class Field<T> {
    /**
     * The logical data type of the field.
     */
    private final Type<T> type;

    /**
     * The field name.
     */
    private final String name;

    /**
     * Creates a new field descriptor with the given type and name.
     *
     * @param type the logical data type of the field (must not be {@code null})
     * @param name the field name used in schemas and record structures
     */
    public Field(final Type<T> type, final String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * Returns the logical data type of this field.
     *
     * @return the type associated with this field
     */
    public Type<T> getType() {
        return this.type;
    }

    /**
     * Returns the name of this field.
     *
     * @return the field name
     */
    public String getName() {
        return this.name;
    }
}
