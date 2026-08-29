/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.query;

import com.kniazkov.widgets.db.DataRecord;
import com.kniazkov.widgets.db.Field;
import java.util.Objects;

/**
 * Ordering by one field.
 */
public final class Order {
    /**
     * Ordered field.
     */
    private final Field<?> field;

    /**
     * Descending flag.
     */
    private final boolean descending;

    /**
     * Creates ordering.
     *
     * @param field field
     * @param descending descending flag
     */
    private Order(final Field<?> field, final boolean descending) {
        this.field = Objects.requireNonNull(field, "field");
        this.descending = descending;
    }

    /**
     * Creates ascending ordering.
     *
     * @param field field
     * @return ordering
     */
    public static Order ascending(final Field<?> field) {
        return new Order(field, false);
    }

    /**
     * Creates descending ordering.
     *
     * @param field field
     * @return ordering
     */
    public static Order descending(final Field<?> field) {
        return new Order(field, true);
    }

    /**
     * Returns the ordered field.
     *
     * @return field
     */
    public Field<?> getField() {
        return this.field;
    }

    /**
     * Compares two records.
     *
     * @param first first record
     * @param second second record
     * @return comparison result
     */
    public int compare(final DataRecord first, final DataRecord second) {
        final int result = compareField(this.field, first, second);
        return this.descending ? -result : result;
    }

    /**
     * Compares records using a typed field.
     *
     * @param field field
     * @param first first record
     * @param second second record
     * @param <T> value type
     * @return comparison result
     */
    private static <T> int compareField(
        final Field<T> field,
        final DataRecord first,
        final DataRecord second
    ) {
        return field.getType().compare(
            first.model(field).getData(),
            second.model(field).getData()
        );
    }
}
