/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.query;

import com.kniazkov.widgets.db.Field;

/**
 * Creates primitive query conditions without exposing their implementation classes.
 */
public final class Conditions {
    /**
     * Utility class.
     */
    private Conditions() {
    }

    /**
     * Creates an equality condition.
     *
     * @param field field
     * @param value expected value
     * @param <T> value type
     * @return condition
     */
    public static <T> Condition equal(final Field<T> field, final T value) {
        return new Comparison<>(field, value, Comparison.Operator.EQUAL);
    }

    /**
     * Creates an inequality condition.
     *
     * @param field field
     * @param value unexpected value
     * @param <T> value type
     * @return condition
     */
    public static <T> Condition notEqual(final Field<T> field, final T value) {
        return new Comparison<>(field, value, Comparison.Operator.NOT_EQUAL);
    }

    /**
     * Creates a greater-than condition.
     *
     * @param field field
     * @param value boundary
     * @param <T> value type
     * @return condition
     */
    public static <T> Condition greaterThan(final Field<T> field, final T value) {
        return new Comparison<>(field, value, Comparison.Operator.GREATER_THAN);
    }

    /**
     * Creates a less-than condition.
     *
     * @param field field
     * @param value boundary
     * @param <T> value type
     * @return condition
     */
    public static <T> Condition lessThan(final Field<T> field, final T value) {
        return new Comparison<>(field, value, Comparison.Operator.LESS_THAN);
    }

    /**
     * Returns a condition that always matches.
     *
     * @return condition
     */
    public static Condition all() {
        return All.INSTANCE;
    }

    /**
     * A condition matching every record.
     */
    private enum All implements Condition {
        /**
         * Shared instance.
         */
        INSTANCE;

        @Override
        public boolean matches(final com.kniazkov.widgets.db.DataRecord record) {
            return true;
        }

        @Override
        public java.util.Set<Field<?>> dependencies() {
            return java.util.Set.of();
        }
    }
}
