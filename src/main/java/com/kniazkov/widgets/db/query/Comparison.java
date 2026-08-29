/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.query;

import com.kniazkov.widgets.db.DataRecord;
import com.kniazkov.widgets.db.Field;
import java.util.Objects;
import java.util.Set;

/**
 * Internal field comparison node.
 *
 * @param <T> value type
 */
final class Comparison<T> implements Condition {
    /**
     * Supported comparison operators.
     */
    enum Operator {
        /**
         * Equality.
         */
        EQUAL,

        /**
         * Inequality.
         */
        NOT_EQUAL,

        /**
         * Greater-than comparison.
         */
        GREATER_THAN,

        /**
         * Less-than comparison.
         */
        LESS_THAN
    }

    /**
     * Field.
     */
    private final Field<T> field;

    /**
     * Right operand.
     */
    private final T value;

    /**
     * Operator.
     */
    private final Operator operator;

    /**
     * Creates a comparison.
     *
     * @param field field
     * @param value right operand
     * @param operator operator
     */
    Comparison(final Field<T> field, final T value, final Operator operator) {
        this.field = Objects.requireNonNull(field, "field");
        this.value = Objects.requireNonNull(value, "value");
        this.operator = Objects.requireNonNull(operator, "operator");
    }

    @Override
    public boolean matches(final DataRecord record) {
        final T current = record.model(this.field).getData();
        return switch (this.operator) {
            case EQUAL -> current.equals(this.value);
            case NOT_EQUAL -> !current.equals(this.value);
            case GREATER_THAN -> this.field.getType().compare(current, this.value) > 0;
            case LESS_THAN -> this.field.getType().compare(current, this.value) < 0;
        };
    }

    @Override
    public Set<Field<?>> dependencies() {
        return Set.of(this.field);
    }
}
