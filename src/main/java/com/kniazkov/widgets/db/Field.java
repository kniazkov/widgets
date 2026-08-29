/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.db.query.Condition;
import com.kniazkov.widgets.db.query.Conditions;
import com.kniazkov.widgets.db.query.Order;
import java.util.Objects;
import java.util.UUID;

/**
 * A typed field in a {@link Schema}.
 *
 * @param <T> value type
 */
public final class Field<T> {
    /**
     * Field name.
     */
    private final String name;

    /**
     * Field value type.
     */
    private final ValueType<T> type;

    /**
     * Referenced store for identifier fields.
     */
    private final String referencedStore;

    /**
     * Creates a field.
     *
     * @param type value type
     * @param name field name
     */
    public Field(final ValueType<T> type, final String name) {
        this(type, name, null);
    }

    /**
     * Creates a field that may reference another store.
     *
     * @param type value type
     * @param name field name
     * @param referencedStore referenced store, or {@code null}
     */
    public Field(
        final ValueType<T> type,
        final String name,
        final String referencedStore
    ) {
        this.type = Objects.requireNonNull(type, "type");
        this.name = Objects.requireNonNull(name, "name");
        if (this.name.isBlank()) {
            throw new IllegalArgumentException("Field name cannot be blank");
        }
        if (referencedStore != null && referencedStore.isBlank()) {
            throw new IllegalArgumentException(
                "Referenced store name cannot be blank"
            );
        }
        if (referencedStore != null
            && !UUID.class.equals(this.type.getValueClass())) {
            throw new IllegalArgumentException(
                "Only UUID fields can reference another store"
            );
        }
        this.referencedStore = referencedStore;
    }

    /**
     * Returns the field name.
     *
     * @return name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the field value type.
     *
     * @return value type
     */
    public ValueType<T> getType() {
        return this.type;
    }

    /**
     * Returns the referenced store for an identifier field.
     *
     * @return referenced store, or {@code null}
     */
    public String getReferencedStore() {
        return this.referencedStore;
    }

    /**
     * Creates an equality condition.
     *
     * @param value expected value
     * @return condition
     */
    public Condition is(final T value) {
        return Conditions.equal(this, value);
    }

    /**
     * Creates an inequality condition.
     *
     * @param value unexpected value
     * @return condition
     */
    public Condition isNot(final T value) {
        return Conditions.notEqual(this, value);
    }

    /**
     * Creates a greater-than condition.
     *
     * @param value boundary
     * @return condition
     */
    public Condition greaterThan(final T value) {
        return Conditions.greaterThan(this, value);
    }

    /**
     * Creates a less-than condition.
     *
     * @param value boundary
     * @return condition
     */
    public Condition lessThan(final T value) {
        return Conditions.lessThan(this, value);
    }

    /**
     * Creates ascending ordering.
     *
     * @return ordering
     */
    public Order ascending() {
        return Order.ascending(this);
    }

    /**
     * Creates descending ordering.
     *
     * @return ordering
     */
    public Order descending() {
        return Order.descending(this);
    }
}
